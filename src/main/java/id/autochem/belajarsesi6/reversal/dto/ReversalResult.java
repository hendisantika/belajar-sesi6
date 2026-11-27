package id.autochem.belajarsesi6.reversal.dto;

import id.autochem.belajarsesi6.journal.JournalEntry;
import id.autochem.belajarsesi6.journal.JournalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Created by IntelliJ IDEA.
 * Project : belajar-sesi6
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Link : s.id/hendisantika
 * Date: 27/11/26
 * Time: 19.44
 */
public record ReversalResult(Long originalId, String originalNumber, LocalDate originalDate,
                             JournalStatus originalStatus, Long reversalId, String reversalNumber,
                             JournalStatus reversalStatus, LocalDate reversalDate, String reason, String reversedBy,
                             Instant reversedAt, int ledgerEntries, BigDecimal amount) {

    public static ReversalResult of(JournalEntry original, JournalEntry reversal, int ledgerEntries) {
        return new ReversalResult(original.getId(), original.getJournalNumber(), original.getJournalDate(),
                original.getStatus(), reversal.getId(), reversal.getJournalNumber(), reversal.getStatus(),
                reversal.getJournalDate(), reversal.getReversalReason(), original.getReversedBy(),
                original.getReversedAt(), ledgerEntries, reversal.totalDebit());
    }
}
