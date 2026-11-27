package id.autochem.belajarsesi6.trialbalance.dto;

import id.autochem.belajarsesi6.account.AccountType;
import id.autochem.belajarsesi6.account.NormalBalance;
import id.autochem.belajarsesi6.journal.JournalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

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
public record TrialBalanceReport(
        LocalDate asOf,
        String source,
        Set<JournalStatus> journalStatusesIncluded,
        int accountCount,
        List<Line> lines,
        List<TypeSubtotal> subtotals,
        Totals totals,
        List<String> warnings,
        Instant generatedAt) {

    public record Line(String code, String name, AccountType accountType, NormalBalance normalBalance,
                       BigDecimal totalDebit, BigDecimal totalCredit,
                       BigDecimal debitBalance, BigDecimal creditBalance,
                       /* Saldo dari sisi normal akun; negatif = saldo abnormal. */
                       BigDecimal normalSideBalance, boolean abnormal) {
    }

    public record TypeSubtotal(AccountType accountType, BigDecimal debitBalance, BigDecimal creditBalance) {
    }

    public record Totals(BigDecimal totalDebit, BigDecimal totalCredit,
                         BigDecimal totalDebitBalance, BigDecimal totalCreditBalance,
                         BigDecimal difference, boolean balanced) {
    }
}
