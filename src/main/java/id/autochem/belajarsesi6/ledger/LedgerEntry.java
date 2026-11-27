package id.autochem.belajarsesi6.ledger;

import id.autochem.belajarsesi6.account.Account;
import id.autochem.belajarsesi6.journal.JournalEntry;
import id.autochem.belajarsesi6.journal.JournalLine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

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
@Getter
@Entity
@Immutable
@Table(name = "ledger_entry")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_id", nullable = false, updatable = false)
    private JournalEntry journal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_line_id", nullable = false, unique = true, updatable = false)
    private JournalLine journalLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    @Column(name = "posting_date", nullable = false, updatable = false)
    private LocalDate postingDate;

    @Column(length = 255, updatable = false)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal debit;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal credit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Satu LedgerEntry per baris jurnal yang diposting; tanggal posting = tanggal jurnal. */
    public static LedgerEntry of(JournalEntry journal, JournalLine line, Instant now) {
        LedgerEntry entry = new LedgerEntry();
        entry.journal = journal;
        entry.journalLine = line;
        entry.account = line.getAccount();
        entry.postingDate = journal.getJournalDate();
        entry.description = line.getDescription() != null ? line.getDescription() : journal.getDescription();
        entry.debit = line.getDebit();
        entry.credit = line.getCredit();
        entry.createdAt = now;
        return entry;
    }
}
