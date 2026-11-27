package id.autochem.belajarsesi6.posting.dto;

import id.autochem.belajarsesi6.journal.JournalEntry;
import id.autochem.belajarsesi6.journal.JournalStatus;
import id.autochem.belajarsesi6.journal.JournalType;

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
public record PostingResult(Long journalId, String journalNumber, JournalType journalType, LocalDate postingDate,
                            JournalStatus status, String postedBy, Instant postedAt, int ledgerEntries,
                            BigDecimal totalDebit, BigDecimal totalCredit) {

    public static PostingResult of(JournalEntry e, int ledgerEntries) {
        return new PostingResult(e.getId(), e.getJournalNumber(), e.getJournalType(), e.getJournalDate(),
                e.getStatus(), e.getPostedBy(), e.getPostedAt(), ledgerEntries, e.totalDebit(), e.totalCredit());
    }
}
