package id.autochem.belajarsesi6.trialbalance;

import id.autochem.belajarsesi6.account.Account;
import id.autochem.belajarsesi6.account.AccountRepository;
import id.autochem.belajarsesi6.account.AccountType;
import id.autochem.belajarsesi6.journal.JournalStatus;
import id.autochem.belajarsesi6.period.AccountingPeriod;
import id.autochem.belajarsesi6.period.PeriodQueryService;
import id.autochem.belajarsesi6.trialbalance.dto.TrialBalanceReport;
import id.autochem.belajarsesi6.trialbalance.dto.WorksheetReport;
import id.autochem.belajarsesi6.trialbalance.dto.WorksheetReport.DebitCredit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by IntelliJ IDEA.
 * Project : belajar-sesi6
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Link : s.id/hendisantika
 * Date: 27/11/26
 * Time: 19.46
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrialBalanceService {

    /** Hanya jurnal yang sudah masuk buku besar; DRAFT & APPROVED selalu dikecualikan. */
    public static final Set<JournalStatus> INCLUDED_STATUSES = JournalStatus.inLedger();

    private final TrialBalanceRepository repository;
    private final AccountRepository accounts;
    private final PeriodQueryService periods;
    private final Clock clock;

    /** Daftar baris neraca saldo s.d. asOf (dipakai juga oleh closing checklist & rekonsiliasi). */
    public List<TrialBalanceLine> lines(LocalDate asOf) {
        return repository.trialBalance(asOf, INCLUDED_STATUSES);
    }

    public TrialBalanceReport trialBalance(LocalDate asOf, boolean includeZero) {
        LocalDate date = asOf != null ? asOf : LocalDate.now(clock);
        List<TrialBalanceLine> lines = lines(date);
        if (includeZero) {
            lines = withZeroAccounts(lines);
        }
        return toReport(date, "LIVE", lines);
    }

    public TrialBalanceReport toReport(LocalDate asOf, String source, List<TrialBalanceLine> lines) {
        List<TrialBalanceReport.Line> rows = lines.stream()
                .map(l -> new TrialBalanceReport.Line(l.code(), l.name(), l.accountType(), l.normalBalance(),
                        l.totalDebit(), l.totalCredit(), l.debitBalance(), l.creditBalance(),
                        l.normalSideBalance(), l.isAbnormal()))
                .toList();

        Map<AccountType, BigDecimal[]> byType = new EnumMap<>(AccountType.class);
        for (TrialBalanceLine l : lines) {
            BigDecimal[] sums = byType.computeIfAbsent(l.accountType(), t -> new BigDecimal[]{zero(), zero()});
            sums[0] = sums[0].add(l.debitBalance());
            sums[1] = sums[1].add(l.creditBalance());
        }
        List<TrialBalanceReport.TypeSubtotal> subtotals = byType.entrySet().stream()
                .map(e -> new TrialBalanceReport.TypeSubtotal(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();

        BigDecimal totalDebit = sum(lines, TrialBalanceLine::totalDebit);
        BigDecimal totalCredit = sum(lines, TrialBalanceLine::totalCredit);
        BigDecimal debitBalance = sum(lines, TrialBalanceLine::debitBalance);
        BigDecimal creditBalance = sum(lines, TrialBalanceLine::creditBalance);
        BigDecimal difference = debitBalance.subtract(creditBalance);
        boolean balanced = difference.signum() == 0 && totalDebit.compareTo(totalCredit) == 0;

        List<String> warnings = new ArrayList<>();
        if (!balanced) {
            warnings.add("Trial balance TIDAK seimbang, selisih " + difference
                    + ". Telusuri lewat GET /api/reconciliation?asOf=" + asOf);
        }
        lines.stream().filter(TrialBalanceLine::isAbnormal).forEach(l -> warnings.add(
                "Saldo abnormal akun " + l.code() + " " + l.name() + ": " + l.normalSideBalance()
                        + " (normal " + l.normalBalance() + ")"));

        return new TrialBalanceReport(asOf, source, INCLUDED_STATUSES, rows.size(), rows, subtotals,
                new TrialBalanceReport.Totals(totalDebit, totalCredit, debitBalance, creditBalance, difference,
                        balanced),
                warnings, clock.instant());
    }

    /** Kertas kerja: neraca saldo sebelum penyesuaian, penyesuaian, dan sesudah penyesuaian. */
    public WorksheetReport worksheet(String periodCode) {
        AccountingPeriod period = periods.require(periodCode);
        List<WorksheetReport.Row> rows = new ArrayList<>();
        DebitCredit movement = DebitCredit.zero();
        DebitCredit unadjusted = DebitCredit.zero();
        DebitCredit adjustment = DebitCredit.zero();
        DebitCredit adjusted = DebitCredit.zero();
        BigDecimal netIncome = zero();

        for (WorksheetRow r : repository.worksheet(period.getStartDate(), period.getEndDate())) {
            DebitCredit mov = new DebitCredit(r.getDebit(), r.getCredit());
            DebitCredit adj = new DebitCredit(r.getAdjustmentDebit(), r.getAdjustmentCredit());
            BigDecimal unadjustedBalance = r.getOpening().add(r.getDebit()).subtract(r.getCredit());
            BigDecimal adjustedBalance = unadjustedBalance.add(r.getAdjustmentDebit()).subtract(r.getAdjustmentCredit());
            DebitCredit unadj = DebitCredit.ofBalance(unadjustedBalance);
            DebitCredit adjd = DebitCredit.ofBalance(adjustedBalance);
            rows.add(new WorksheetReport.Row(r.getCode(), r.getName(), r.getAccountType(), r.getNormalBalance(),
                    r.getOpening(), mov, unadj, adj, adjd));
            movement = movement.plus(mov);
            unadjusted = unadjusted.plus(unadj);
            adjustment = adjustment.plus(adj);
            adjusted = adjusted.plus(adjd);

            // Laba periode: mutasi (reguler + penyesuaian) akun pendapatan & beban di periode ini
            BigDecimal periodMovement = r.getDebit().add(r.getAdjustmentDebit())
                    .subtract(r.getCredit()).subtract(r.getAdjustmentCredit());
            if (AccountType.REVENUE.name().equals(r.getAccountType())
                    || AccountType.EXPENSE.name().equals(r.getAccountType())) {
                netIncome = netIncome.subtract(periodMovement);
            }
        }
        return new WorksheetReport(period.getCode(), period.getStartDate(), period.getEndDate(), period.getStatus(),
                rows, new WorksheetReport.Columns(movement, unadjusted, adjustment, adjusted),
                unadjusted.balanced(), adjusted.balanced(), netIncome);
    }

    private List<TrialBalanceLine> withZeroAccounts(List<TrialBalanceLine> lines) {
        Map<String, TrialBalanceLine> byCode = lines.stream()
                .collect(Collectors.toMap(TrialBalanceLine::code, Function.identity(), (a, b) -> a, TreeMap::new));
        for (Account a : accounts.findByActiveOrderByCodeAsc(true)) {
            byCode.putIfAbsent(a.getCode(), TrialBalanceLine.zero(a.getCode(), a.getName(), a.getAccountType(),
                    a.getNormalBalance()));
        }
        return List.copyOf(byCode.values());
    }

    private static BigDecimal sum(List<TrialBalanceLine> lines, Function<TrialBalanceLine, BigDecimal> f) {
        return lines.stream().map(f).reduce(zero(), BigDecimal::add);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2);
    }
}
