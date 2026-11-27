package id.autochem.belajarsesi6.reconciliation;

import id.autochem.belajarsesi6.account.AccountRepository;
import id.autochem.belajarsesi6.audit.AuditEventRepository;
import id.autochem.belajarsesi6.audit.EntityTypes;
import id.autochem.belajarsesi6.common.exception.NotFoundException;
import id.autochem.belajarsesi6.reconciliation.ReconciliationRepository.AccountComparison;
import id.autochem.belajarsesi6.reconciliation.ReconciliationRepository.Finding;
import id.autochem.belajarsesi6.reconciliation.ReconciliationRepository.TraceRow;
import id.autochem.belajarsesi6.reconciliation.dto.AccountTraceReport;
import id.autochem.belajarsesi6.reconciliation.dto.ReconciliationReport;
import id.autochem.belajarsesi6.reconciliation.dto.ReconciliationReport.DebitCredit;
import id.autochem.belajarsesi6.reconciliation.dto.ReconciliationReport.FindingGroup;
import id.autochem.belajarsesi6.reconciliation.dto.ReconciliationReport.FindingItem;
import id.autochem.belajarsesi6.trialbalance.TrialBalanceService;
import id.autochem.belajarsesi6.trialbalance.dto.TrialBalanceReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by IntelliJ IDEA.
 * Project : belajar-sesi6
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Link : s.id/hendisantika
 * Date: 27/11/26
 * Time: 19.52
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReconciliationService {

    private static final String ERROR = "ERROR";
    private static final String WARNING = "WARNING";
    private static final String INFO = "INFO";

    private final ReconciliationRepository repository;
    private final TrialBalanceService trialBalance;
    private final AccountRepository accounts;
    private final AuditEventRepository auditEvents;
    private final Clock clock;

    public ReconciliationReport reconcile(LocalDate asOf, boolean includeMatched) {
        LocalDate date = asOf != null ? asOf : LocalDate.now(clock);
        List<AccountComparison> rows = repository.compareAccounts(date);

        List<ReconciliationReport.AccountCheck> checks = rows.stream().map(r -> {
            DebitCredit tb = new DebitCredit(r.getTbDebit(), r.getTbCredit());
            DebitCredit gl = new DebitCredit(r.getGlDebit(), r.getGlCredit());
            DebitCredit jl = new DebitCredit(r.getJournalDebit(), r.getJournalCredit());
            boolean tbGl = same(tb, gl);
            boolean glJl = same(gl, jl);
            BigDecimal diff = net(tb).subtract(net(jl));
            return new ReconciliationReport.AccountCheck(r.getCode(), r.getName(), tb, gl, jl, tbGl, glJl, diff);
        }).toList();

        List<FindingGroup> findings = new ArrayList<>();
        long mismatchedAccounts = checks.stream().filter(c -> !c.matched()).count();
        if (mismatchedAccounts > 0) {
            findings.add(new FindingGroup("ACCOUNT_MISMATCH", ERROR, "Saldo akun TB / GL / jurnal tidak sama",
                    "Telusuri akun lewat GET /api/reconciliation/accounts/{code}/trace?asOf=" + date,
                    (int) mismatchedAccounts, checks.stream().filter(c -> !c.matched())
                    .map(c -> new FindingItem(null, c.code(), null, date, c.name()
                            + (c.tbMatchesGl() ? "" : " | TB != GL") + (c.glMatchesJournal() ? "" : " | GL != jurnal"),
                            c.difference()))
                    .toList()));
        }
        addFinding(findings, "LEDGER_FROM_UNPOSTED_JOURNAL", ERROR,
                "Ledger berasal dari jurnal yang belum POSTED",
                "Penyebab umum: jurnal draft ikut dihitung. GL mentah memuat mutasi jurnal DRAFT/APPROVED",
                repository.ledgerFromUnpostedJournals(date));
        addFinding(findings, "POSTING_DATE_MISMATCH", ERROR,
                "Tanggal posting GL berbeda dengan tanggal jurnal",
                "Penyebab umum: filter periode tidak konsisten. TB memfilter posting_date, jurnal memfilter journal_date",
                repository.postingDateMismatches(date));
        addFinding(findings, "INCOMPLETE_LEDGER", ERROR,
                "Jurnal POSTED tanpa ledger lengkap / ledger ganda",
                "Jumlah ledger entry harus sama dengan jumlah baris jurnal",
                repository.postedWithoutCompleteLedger(date));
        addFinding(findings, "REVERSAL_INTEGRITY", ERROR,
                "Reversal tanpa jurnal asal / jurnal REVERSED tanpa pembalik",
                "Penyebab umum: reversal tanpa jurnal asal. Koreksi harus selalu menunjuk jurnal asal",
                repository.reversalIntegrityIssues());
        addFinding(findings, "UNBALANCED_JOURNAL", ERROR, "Jurnal POSTED tidak seimbang",
                "Debit dan kredit setiap jurnal POSTED wajib sama", repository.unbalancedJournals(date));
        List<ReconciliationRepository.Drift> drift = repository.lockedPeriodDrift(date);
        if (!drift.isEmpty()) {
            findings.add(new FindingGroup("LOCKED_PERIOD_DRIFT", ERROR,
                    "Saldo periode LOCKED berubah dibanding snapshot",
                    "Laporan yang sudah ditutup tidak boleh berubah diam-diam; cek audit trail periode & jurnal",
                    drift.size(), drift.stream().map(d -> new FindingItem(null, d.getPeriod() + "/" + d.getCode(),
                    "LOCKED", null, "snapshot " + d.getSnapshotBalance() + " vs live " + d.getLiveBalance(),
                    d.getLiveBalance().subtract(d.getSnapshotBalance()))).toList()));
        }
        addFinding(findings, "UNPOSTED_JOURNALS_EXCLUDED", INFO,
                "Jurnal DRAFT/APPROVED dikecualikan dari trial balance",
                "Benar dikecualikan. Bila laporan lain menyertakannya, di sinilah sumber selisihnya",
                repository.unpostedJournals(date));

        TrialBalanceReport tb = trialBalance.toReport(date, "LIVE", trialBalance.lines(date));
        DebitCredit tbTotal = new DebitCredit(sum(checks, c -> c.trialBalance().debit()),
                sum(checks, c -> c.trialBalance().credit()));
        DebitCredit glTotal = new DebitCredit(sum(checks, c -> c.generalLedger().debit()),
                sum(checks, c -> c.generalLedger().credit()));
        DebitCredit jlTotal = new DebitCredit(sum(checks, c -> c.postedJournalLines().debit()),
                sum(checks, c -> c.postedJournalLines().credit()));

        boolean reconciled = tb.totals().balanced() && findings.stream().noneMatch(f -> ERROR.equals(f.severity()));
        return new ReconciliationReport(date, reconciled ? "RECONCILED" : "MISMATCH",
                new ReconciliationReport.Layers(tbTotal, glTotal, jlTotal, tb.totals().balanced()),
                checks.size(),
                includeMatched ? checks : checks.stream().filter(c -> !c.matched()).toList(),
                findings, clock.instant());
    }

    public AccountTraceReport trace(String accountCode, LocalDate asOf) {
        LocalDate date = asOf != null ? asOf : LocalDate.now(clock);
        accounts.findByCode(accountCode).orElseThrow(() -> new NotFoundException("Akun", accountCode));

        TrialBalanceReport.Line tbLine = trialBalance.toReport(date, "LIVE", trialBalance.lines(date)).lines()
                .stream().filter(l -> l.code().equals(accountCode)).findFirst().orElse(null);

        List<TraceRow> rows = repository.trace(accountCode, date);
        List<AccountTraceReport.Entry> entries = rows.stream()
                .map(r -> new AccountTraceReport.Entry(r.getLedgerEntryId(), r.getPostingDate(), r.getJournalId(),
                        r.getJournalNumber(), r.getJournalType(), r.getJournalStatus(), r.getJournalDate(),
                        r.getLineNo(), r.getDescription(), r.getDebit(), r.getCredit(), r.getRunningBalance(),
                        r.getPostedBy(),
                        r.getDebit().compareTo(r.getLineDebit()) == 0 && r.getCredit().compareTo(r.getLineCredit()) == 0
                                && r.getPostingDate().equals(r.getJournalDate())))
                .toList();

        BigDecimal ledgerBalance = rows.stream()
                .filter(r -> "POSTED".equals(r.getJournalStatus()) || "REVERSED".equals(r.getJournalStatus()))
                .map(r -> r.getDebit().subtract(r.getCredit())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lineBalance = rows.stream()
                .filter(r -> ("POSTED".equals(r.getJournalStatus()) || "REVERSED".equals(r.getJournalStatus()))
                        && !r.getJournalDate().isAfter(date))
                .map(r -> r.getLineDebit().subtract(r.getLineCredit())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tbBalance = tbLine == null ? BigDecimal.ZERO : tbLine.totalDebit().subtract(tbLine.totalCredit());
        boolean consistent = tbBalance.compareTo(ledgerBalance) == 0 && ledgerBalance.compareTo(lineBalance) == 0
                && entries.stream().allMatch(AccountTraceReport.Entry::matchesJournalLine);

        Map<Long, String> journalIds = new LinkedHashMap<>();
        rows.forEach(r -> journalIds.putIfAbsent(r.getJournalId(), r.getJournalNumber()));
        List<AccountTraceReport.JournalAudit> audit = journalIds.entrySet().stream()
                .map(e -> new AccountTraceReport.JournalAudit(e.getValue(),
                        auditEvents.findByEntityTypeAndEntityIdOrderByOccurredAtAscIdAsc(EntityTypes.JOURNAL,
                                        e.getKey()).stream()
                                .map(a -> new AccountTraceReport.Event(a.getAction().name(), a.getOutcome().name(),
                                        a.getActor(), a.getReason(), a.getOccurredAt()))
                                .toList()))
                .toList();

        return new AccountTraceReport(accountCode, date, tbLine, ledgerBalance, lineBalance, consistent,
                entries.size(), entries, repository.excludedLines(accountCode, date).stream().map(this::item).toList(),
                audit);
    }

    private void addFinding(List<FindingGroup> findings, String code, String severity, String title,
                            String explanation, List<Finding> rows) {
        if (!rows.isEmpty()) {
            findings.add(new FindingGroup(code, severity, title, explanation, rows.size(),
                    rows.stream().map(this::item).toList()));
        }
    }

    private FindingItem item(Finding f) {
        return new FindingItem(f.getJournalId(), f.getJournalNumber(), f.getStatus(), f.getJournalDate(),
                f.getDetail(), f.getAmount());
    }

    private static boolean same(DebitCredit a, DebitCredit b) {
        return a.debit().compareTo(b.debit()) == 0 && a.credit().compareTo(b.credit()) == 0;
    }

    private static BigDecimal net(DebitCredit dc) {
        return dc.debit().subtract(dc.credit());
    }

    private static <T> BigDecimal sum(List<T> rows, Function<T, BigDecimal> f) {
        return rows.stream().map(f).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }
}
