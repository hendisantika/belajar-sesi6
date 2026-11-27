package id.autochem.belajarsesi6.reconciliation.dto;

import id.autochem.belajarsesi6.trialbalance.dto.TrialBalanceReport;

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
/** Drill-down satu akun: baris Trial Balance -> ledger entry GL -> baris jurnal -> audit trail. */
public record AccountTraceReport(
        String accountCode,
        LocalDate asOf,
        TrialBalanceReport.Line trialBalanceLine,
        BigDecimal ledgerBalance,
        BigDecimal postedLineBalance,
        boolean consistent,
        int entryCount,
        List<Entry> entries,
        List<ReconciliationReport.FindingItem> excludedUnpostedLines,
        List<JournalAudit> journalAudit) {

    public record Entry(Long ledgerEntryId, LocalDate postingDate, Long journalId, String journalNumber,
                        String journalType, String journalStatus, LocalDate journalDate, Integer lineNo,
                        String description, BigDecimal debit, BigDecimal credit, BigDecimal runningBalance,
                        String postedBy, boolean matchesJournalLine) {
    }

    public record JournalAudit(String journalNumber, List<Event> events) {
    }

    public record Event(String action, String outcome, String actor, String reason, Instant occurredAt) {
    }
}
