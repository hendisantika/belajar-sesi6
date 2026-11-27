package id.autochem.belajarsesi6.journal.dto;

import id.autochem.belajarsesi6.journal.JournalEntry;
import id.autochem.belajarsesi6.journal.JournalStatus;
import id.autochem.belajarsesi6.journal.JournalType;

import java.math.BigDecimal;
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
public record JournalSummaryResponse(Long id, String journalNumber, LocalDate journalDate, String description,
                                     JournalType journalType, JournalStatus status, BigDecimal totalDebit,
                                     BigDecimal totalCredit) {

    public static JournalSummaryResponse from(JournalEntry e) {
        return new JournalSummaryResponse(e.getId(), e.getJournalNumber(), e.getJournalDate(), e.getDescription(),
                e.getJournalType(), e.getStatus(), e.totalDebit(), e.totalCredit());
    }
}
