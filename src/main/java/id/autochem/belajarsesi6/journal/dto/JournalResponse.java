package id.autochem.belajarsesi6.journal.dto;

import id.autochem.belajarsesi6.journal.JournalEntry;
import id.autochem.belajarsesi6.journal.JournalStatus;
import id.autochem.belajarsesi6.journal.JournalType;

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
 * Time: 19.44
 */
public record JournalResponse(
        Long id,
        String journalNumber,
        LocalDate journalDate,
        String description,
        JournalType journalType,
        JournalStatus status,
        boolean autoReverse,
        Set<JournalStatus> allowedTransitions,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        boolean balanced,
        Reference reversalOf,
        Reference reversedBy,
        String reversalReason,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt,
        String approvedBy,
        Instant approvedAt,
        String postedBy,
        Instant postedAt,
        String reversedByUser,
        Instant reversedAt,
        Long version,
        List<JournalLineResponse> lines) {

    public record Reference(Long id, String journalNumber) {
        static Reference of(JournalEntry e) {
            return e == null ? null : new Reference(e.getId(), e.getJournalNumber());
        }
    }

    public static JournalResponse from(JournalEntry e) {
        return from(e, null);
    }

    public static JournalResponse from(JournalEntry e, JournalEntry reversal) {
        return new JournalResponse(
                e.getId(), e.getJournalNumber(), e.getJournalDate(), e.getDescription(), e.getJournalType(),
                e.getStatus(), e.isAutoReverse(), e.getStatus().allowedTransitions(), e.totalDebit(),
                e.totalCredit(), e.isBalanced(), Reference.of(e.getReversalOf()), Reference.of(reversal),
                e.getReversalReason(), e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedBy(), e.getUpdatedAt(),
                e.getApprovedBy(), e.getApprovedAt(), e.getPostedBy(), e.getPostedAt(), e.getReversedBy(),
                e.getReversedAt(), e.getVersion(), e.getLines().stream().map(JournalLineResponse::from).toList());
    }
}
