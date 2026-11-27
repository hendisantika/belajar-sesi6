package id.autochem.belajarsesi6.reconciliation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
public record ReconciliationReport(
        LocalDate asOf,
        /* RECONCILED bila TB = GL = jurnal & tidak ada temuan ERROR; selain itu MISMATCH. */
        String status,
        Layers totals,
        int accountsChecked,
        List<AccountCheck> accounts,
        List<FindingGroup> findings,
        Instant generatedAt) {

    public record Layers(DebitCredit trialBalance, DebitCredit generalLedger, DebitCredit postedJournalLines,
                         boolean trialBalanceBalanced) {
    }

    public record DebitCredit(BigDecimal debit, BigDecimal credit) {
    }

    public record AccountCheck(String code, String name, DebitCredit trialBalance, DebitCredit generalLedger,
                               DebitCredit postedJournalLines, boolean tbMatchesGl, boolean glMatchesJournal,
                               BigDecimal difference) {

        public boolean matched() {
            return tbMatchesGl && glMatchesJournal;
        }
    }

    public record FindingGroup(String code, String severity, String title, String explanation, int count,
                               List<FindingItem> items) {
    }

    public record FindingItem(Long journalId, String reference, String status, LocalDate date, String detail,
                              BigDecimal amount) {
    }
}
