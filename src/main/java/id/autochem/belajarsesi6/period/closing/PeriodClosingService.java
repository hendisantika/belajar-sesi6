package id.autochem.belajarsesi6.period.closing;

import id.autochem.belajarsesi6.audit.AuditAction;
import id.autochem.belajarsesi6.audit.AuditTrail;
import id.autochem.belajarsesi6.audit.AuditedOperation;
import id.autochem.belajarsesi6.audit.EntityTypes;
import id.autochem.belajarsesi6.common.exception.BusinessException;
import id.autochem.belajarsesi6.common.exception.ErrorCode;
import id.autochem.belajarsesi6.common.exception.NotFoundException;
import id.autochem.belajarsesi6.journal.JournalEntry;
import id.autochem.belajarsesi6.journal.JournalEntryRepository;
import id.autochem.belajarsesi6.journal.JournalStatus;
import id.autochem.belajarsesi6.period.AccountingPeriod;
import id.autochem.belajarsesi6.period.AccountingPeriodRepository;
import id.autochem.belajarsesi6.period.PeriodStatus;
import id.autochem.belajarsesi6.period.closing.dto.AutoReversalResult;
import id.autochem.belajarsesi6.period.closing.dto.ChecklistItem;
import id.autochem.belajarsesi6.period.closing.dto.ClosingChecklist;
import id.autochem.belajarsesi6.period.closing.dto.LockResult;
import id.autochem.belajarsesi6.period.closing.dto.PeriodBalanceResponse;
import id.autochem.belajarsesi6.period.dto.PeriodResponse;
import id.autochem.belajarsesi6.reversal.ReversalService;
import id.autochem.belajarsesi6.reversal.dto.ReversalResult;
import id.autochem.belajarsesi6.reversal.dto.ReverseRequest;
import id.autochem.belajarsesi6.trialbalance.TrialBalanceLine;
import id.autochem.belajarsesi6.trialbalance.TrialBalanceService;
import id.autochem.belajarsesi6.trialbalance.dto.TrialBalanceReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by IntelliJ IDEA.
 * Project : belajar-sesi6
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Link : s.id/hendisantika
 * Date: 27/11/26
 * Time: 19.48
 */
/**
 * Period closing: cut-off -> closing checklist -> lock (ADMIN) -> snapshot saldo.
 * Unlock / reopen wajib alasan dan tercatat di audit trail.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PeriodClosingService {

    private final AccountingPeriodRepository periods;
    private final PeriodBalanceRepository balances;
    private final JournalEntryRepository journals;
    private final TrialBalanceService trialBalance;
    private final ReversalService reversalService;
    private final AuditTrail audit;
    private final Clock clock;

    // ------------------------------------------------------------------ checklist

    @Transactional(readOnly = true)
    public ClosingChecklist checklist(String code) {
        return checklist(require(code));
    }

    ClosingChecklist checklist(AccountingPeriod period) {
        List<ChecklistItem> items = new ArrayList<>();
        LocalDate today = LocalDate.now(clock);

        items.add(ChecklistItem.blocking("PERIOD_ENDED", "Periode sudah berakhir (hari ini > tanggal akhir)",
                today.isAfter(period.getEndDate()), "Hari ini " + today + ", akhir periode " + period.getEndDate()));

        items.add(ChecklistItem.blocking("CUT_OFF_DONE", "Cut-off sudah dilakukan (status SOFT_CLOSED)",
                period.getStatus() == PeriodStatus.SOFT_CLOSED, "Status saat ini " + period.getStatus()));

        List<AccountingPeriod> previousOpen = periods.findByEndDateBeforeAndStatusNotOrderByStartDateAsc(
                period.getStartDate(), PeriodStatus.LOCKED);
        items.add(ChecklistItem.blocking("PREVIOUS_PERIODS_LOCKED", "Semua periode sebelumnya sudah LOCKED",
                previousOpen.isEmpty(), previousOpen.isEmpty() ? "OK"
                        : "Belum LOCKED: " + previousOpen.stream().map(AccountingPeriod::getCode).toList()));

        List<JournalEntry> drafts = journalsIn(period, EnumSet.of(JournalStatus.DRAFT));
        items.add(ChecklistItem.blocking("NO_DRAFT_JOURNALS", "Tidak ada jurnal DRAFT di periode ini",
                drafts.isEmpty(), numbers(drafts)));

        List<JournalEntry> approved = journalsIn(period, EnumSet.of(JournalStatus.APPROVED));
        items.add(ChecklistItem.blocking("NO_UNPOSTED_APPROVED", "Semua jurnal APPROVED sudah diposting",
                approved.isEmpty(), numbers(approved)));

        TrialBalanceReport tb = trialBalance.toReport(period.getEndDate(), "LIVE",
                trialBalance.lines(period.getEndDate()));
        items.add(ChecklistItem.blocking("TRIAL_BALANCE_BALANCED", "Trial balance per akhir periode seimbang",
                tb.totals().balanced(), "Debit " + tb.totals().totalDebitBalance() + " vs kredit "
                        + tb.totals().totalCreditBalance()));

        long mismatches = balances.countLedgerMismatches(period.getStartDate(), period.getEndDate());
        items.add(ChecklistItem.blocking("LEDGER_MATCHES_JOURNALS",
                "Mutasi General Ledger periode ini = baris jurnal POSTED", mismatches == 0,
                mismatches + " akun tidak cocok"));

        List<JournalEntry> autoReverse = journals.findAutoReversibleAdjustments(period.getStartDate(),
                period.getEndDate());
        Optional<AccountingPeriod> next = periods.findFirstByStartDateAfterOrderByStartDateAsc(period.getEndDate());
        boolean nextReady = autoReverse.isEmpty()
                || next.map(p -> p.getStatus() != PeriodStatus.LOCKED).orElse(false);
        items.add(ChecklistItem.advisory("AUTO_REVERSAL_PENDING",
                "Jurnal penyesuaian auto-reverse siap dibalik di periode berikutnya", nextReady,
                autoReverse.isEmpty() ? "Tidak ada" : autoReverse.size() + " jurnal: " + numbers(autoReverse)
                        + " -> jalankan POST /api/periods/" + period.getCode() + "/auto-reversals"));

        boolean ready = items.stream().allMatch(i -> !i.blocking() || i.passed());
        return new ClosingChecklist(period.getCode(), period.getStartDate(), period.getEndDate(), period.getStatus(),
                ready, items);
    }

    // ------------------------------------------------------------------ transitions

    @Transactional
    @AuditedOperation(action = AuditAction.PERIOD_CUT_OFF, entityType = EntityTypes.PERIOD, entityRef = "#code")
    public PeriodResponse cutOff(String code, String actor) {
        AccountingPeriod period = requireForUpdate(code);
        Map<String, Object> before = period.snapshot();
        period.cutOff(actor, clock.instant());
        periods.saveAndFlush(period);
        audit.success(AuditAction.PERIOD_CUT_OFF, EntityTypes.PERIOD, period.getId(), code, before, period.snapshot());
        return PeriodResponse.from(period);
    }

    @Transactional
    @AuditedOperation(action = AuditAction.PERIOD_REOPENED, entityType = EntityTypes.PERIOD, entityRef = "#code")
    public PeriodResponse reopen(String code, String reason) {
        AccountingPeriod period = requireForUpdate(code);
        Map<String, Object> before = period.snapshot();
        period.reopen();
        periods.saveAndFlush(period);
        audit.success(AuditAction.PERIOD_REOPENED, EntityTypes.PERIOD, period.getId(), code, reason,
                before, period.snapshot());
        return PeriodResponse.from(period);
    }

    /** Lock hanya setelah checklist cut-off lolos; saldo seluruh akun di-snapshot. */
    @Transactional
    @AuditedOperation(action = AuditAction.PERIOD_LOCKED, entityType = EntityTypes.PERIOD, entityRef = "#code")
    public LockResult lock(String code, String actor) {
        AccountingPeriod period = requireForUpdate(code);
        Map<String, Object> before = period.snapshot();
        period.getStatus().transitionTo(PeriodStatus.LOCKED);

        ClosingChecklist checklist = checklist(period);
        if (!checklist.readyToLock()) {
            List<ChecklistItem> failed = checklist.failedBlocking();
            throw new BusinessException(ErrorCode.PERIOD_CHECKLIST_FAILED,
                    "Periode " + code + " belum bisa di-lock: " + failed.stream()
                            .map(i -> i.code() + " (" + i.detail() + ")").collect(Collectors.joining("; ")))
                    .with("failedItems", failed);
        }

        Instant now = clock.instant();
        period.lock(actor, now);
        periods.saveAndFlush(period);
        balances.deleteByPeriod(period.getId());
        int snapshotAccounts = balances.snapshot(period.getId(), period.getStartDate(), period.getEndDate(), actor, now);

        TrialBalanceReport tb = trialBalance.toReport(period.getEndDate(), "LIVE",
                trialBalance.lines(period.getEndDate()));
        Map<String, Object> after = period.snapshot();
        after.put("snapshotAccounts", snapshotAccounts);
        after.put("totalDebitBalance", tb.totals().totalDebitBalance());
        after.put("totalCreditBalance", tb.totals().totalCreditBalance());
        audit.success(AuditAction.PERIOD_LOCKED, EntityTypes.PERIOD, period.getId(), code, before, after);
        log.info("Period {} locked by {} ({} account snapshots)", code, actor, snapshotAccounts);
        return new LockResult(PeriodResponse.from(period), snapshotAccounts, tb.totals().totalDebitBalance(),
                tb.totals().totalCreditBalance(), checklist);
    }

    /** Unlock wajib alasan; periode kembali SOFT_CLOSED sehingga hanya koreksi yang boleh masuk. */
    @Transactional
    @AuditedOperation(action = AuditAction.PERIOD_UNLOCKED, entityType = EntityTypes.PERIOD, entityRef = "#code")
    public PeriodResponse unlock(String code, String reason, String actor) {
        AccountingPeriod period = requireForUpdate(code);
        Map<String, Object> before = period.snapshot();
        periods.findFirstByStartDateAfterOrderByStartDateAsc(period.getEndDate())
                .filter(next -> next.getStatus() == PeriodStatus.LOCKED)
                .ifPresent(next -> {
                    throw new BusinessException(ErrorCode.PERIOD_SEQUENCE_VIOLATION,
                            "Periode berikutnya " + next.getCode() + " masih LOCKED; unlock dari periode terakhir dulu");
                });
        period.unlock(actor, reason, clock.instant());
        periods.saveAndFlush(period);
        int removed = balances.deleteByPeriod(period.getId());
        Map<String, Object> after = period.snapshot();
        after.put("snapshotRemoved", removed);
        audit.success(AuditAction.PERIOD_UNLOCKED, EntityTypes.PERIOD, period.getId(), code, reason, before, after);
        log.warn("Period {} UNLOCKED by {}: {}", code, actor, reason);
        return PeriodResponse.from(period);
    }

    /**
     * Membalik semua jurnal ADJUSTMENT ber-flag autoReverse (mis. akrual) di periode ini
     * pada tanggal pertama periode berikutnya. Aman diulang: jurnal yang sudah REVERSED dilewati.
     */
    @Transactional
    @AuditedOperation(action = AuditAction.PERIOD_AUTO_REVERSAL, entityType = EntityTypes.PERIOD, entityRef = "#code")
    public AutoReversalResult autoReverse(String code, String actor) {
        AccountingPeriod period = require(code);
        AccountingPeriod next = periods.findFirstByStartDateAfterOrderByStartDateAsc(period.getEndDate())
                .orElseThrow(() -> new BusinessException(ErrorCode.PERIOD_NOT_FOUND,
                        "Periode setelah " + code + " belum dibuat"));
        List<ReversalResult> results = new ArrayList<>();
        for (JournalEntry adjustment : journals.findAutoReversibleAdjustments(period.getStartDate(),
                period.getEndDate())) {
            results.add(reversalService.reverse(adjustment.getId(), new ReverseRequest(
                    "Auto-reversal penyesuaian " + code, next.getStartDate()), actor));
        }
        Map<String, Object> after = new java.util.LinkedHashMap<>();
        after.put("targetPeriod", next.getCode());
        after.put("reversed", results.stream().map(ReversalResult::originalNumber).toList());
        audit.success(AuditAction.PERIOD_AUTO_REVERSAL, EntityTypes.PERIOD, period.getId(), code, null, after);
        return new AutoReversalResult(code, next.getCode(), next.getStartDate(), results.size(), results);
    }

    // ------------------------------------------------------------------ snapshot

    @Transactional(readOnly = true)
    public List<PeriodBalanceResponse> snapshot(String code) {
        return balances.findByPeriod(require(code).getId()).stream().map(PeriodBalanceResponse::from).toList();
    }

    /** Neraca saldo periode LOCKED dibaca dari snapshot (tanpa agregasi ulang ledger). */
    @Transactional(readOnly = true)
    public TrialBalanceReport snapshotTrialBalance(String code) {
        AccountingPeriod period = require(code);
        if (period.getStatus() != PeriodStatus.LOCKED) {
            throw new BusinessException(ErrorCode.INVALID_PERIOD_TRANSITION,
                    "Snapshot hanya tersedia untuk periode LOCKED; " + code + " berstatus " + period.getStatus());
        }
        List<TrialBalanceLine> lines = balances.findByPeriod(period.getId()).stream()
                .map(b -> new TrialBalanceLine(b.getAccount().getCode(), b.getAccount().getName(),
                        b.getAccount().getAccountType(), b.getAccount().getNormalBalance(),
                        b.getClosingBalance().max(BigDecimal.ZERO), b.getClosingBalance().min(BigDecimal.ZERO).negate()))
                .toList();
        return trialBalance.toReport(period.getEndDate(), "SNAPSHOT", lines);
    }

    // ------------------------------------------------------------------ helpers

    private List<JournalEntry> journalsIn(AccountingPeriod period, EnumSet<JournalStatus> statuses) {
        return journals.findByJournalDateBetweenAndStatusInOrderByJournalDateAscIdAsc(
                period.getStartDate(), period.getEndDate(), statuses);
    }

    private static String numbers(List<JournalEntry> entries) {
        return entries.isEmpty() ? "OK"
                : entries.stream().map(JournalEntry::getJournalNumber).collect(Collectors.joining(", "));
    }

    private AccountingPeriod require(String code) {
        return periods.findByCode(code).orElseThrow(() -> new NotFoundException("Periode", code));
    }

    private AccountingPeriod requireForUpdate(String code) {
        return periods.findByCodeForUpdate(code).orElseThrow(() -> new NotFoundException("Periode", code));
    }
}
