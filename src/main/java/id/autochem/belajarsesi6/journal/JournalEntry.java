package id.autochem.belajarsesi6.journal;

import id.autochem.belajarsesi6.common.exception.BusinessException;
import id.autochem.belajarsesi6.common.exception.ErrorCode;
import id.autochem.belajarsesi6.common.persistence.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
@Audited
@Table(name = "journal_entry")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JournalEntry extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "journal_number", nullable = false, unique = true, length = 40)
    private String journalNumber;

    @Column(name = "journal_date", nullable = false)
    private LocalDate journalDate;

    @Column(nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "journal_type", nullable = false, length = 12)
    private JournalType journalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private JournalStatus status;

    /** Jurnal penyesuaian (mis. akrual) yang otomatis dibalik di awal periode berikutnya. */
    @Column(name = "auto_reverse", nullable = false)
    private boolean autoReverse;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_id", unique = true)
    private JournalEntry reversalOf;

    @Column(name = "reversal_reason", length = 255)
    private String reversalReason;

    @OneToMany(mappedBy = "journal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<JournalLine> lines = new ArrayList<>();

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "posted_by", length = 100)
    private String postedBy;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "reversed_by", length = 100)
    private String reversedBy;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @Version
    private Long version;

    public static JournalEntry draft(String journalNumber, LocalDate journalDate, String description,
                                     JournalType type, boolean autoReverse) {
        if (autoReverse && type != JournalType.ADJUSTMENT) {
            throw new BusinessException(ErrorCode.INVALID_JOURNAL_TYPE,
                    "autoReverse hanya untuk jurnal ADJUSTMENT (mis. akrual), bukan " + type);
        }
        JournalEntry entry = new JournalEntry();
        entry.journalNumber = journalNumber;
        entry.journalDate = journalDate;
        entry.description = description;
        entry.journalType = type;
        entry.autoReverse = autoReverse;
        entry.status = JournalStatus.DRAFT;
        return entry;
    }

    // ------------------------------------------------------------------ lines

    public List<JournalLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public void addLine(JournalLine line) {
        assertEditable();
        line.attach(this, lines.size() + 1);
        lines.add(line);
    }

    /**
     * Menghapus semua baris (hanya DRAFT). Caller wajib flush sebelum menambah baris baru
     * karena Hibernate mengeksekusi INSERT sebelum DELETE (unique journal_id + line_no).
     */
    public void clearLines() {
        assertEditable();
        lines.clear();
    }

    public void updateHeader(LocalDate journalDate, String description, boolean autoReverse) {
        assertEditable();
        if (autoReverse && journalType != JournalType.ADJUSTMENT) {
            throw new BusinessException(ErrorCode.INVALID_JOURNAL_TYPE, "autoReverse hanya untuk jurnal ADJUSTMENT");
        }
        this.journalDate = journalDate;
        this.description = description;
        this.autoReverse = autoReverse;
    }

    public BigDecimal totalDebit() {
        return lines.stream().map(JournalLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalCredit() {
        return lines.stream().map(JournalLine::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isBalanced() {
        return totalDebit().compareTo(totalCredit()) == 0;
    }

    // -------------------------------------------------------------- invariants

    public void assertBalanced() {
        if (lines.size() < 2 || totalDebit().signum() == 0) {
            throw new BusinessException(ErrorCode.JOURNAL_EMPTY,
                    "Jurnal " + journalNumber + " minimal dua baris dengan total > 0");
        }
        if (!isBalanced()) {
            throw new BusinessException(ErrorCode.JOURNAL_UNBALANCED,
                    "Jurnal " + journalNumber + " tidak seimbang: debit " + totalDebit() + " != kredit " + totalCredit());
        }
    }

    public void assertEditable() {
        if (status != null && !status.isEditable()) {
            throw new BusinessException(ErrorCode.JOURNAL_NOT_EDITABLE,
                    "Jurnal " + journalNumber + " berstatus " + status
                            + "; hanya DRAFT yang dapat diubah. Jurnal POSTED dikoreksi lewat reversal/adjustment");
        }
    }

    // -------------------------------------------------------------- lifecycle

    /** DRAFT -> APPROVED. Maker-checker: approver harus berbeda dari pembuat jurnal. */
    public void approve(String approver, Instant now) {
        JournalStatus next = status.transitionTo(JournalStatus.APPROVED);
        if (Objects.equals(getCreatedBy(), approver)) {
            throw new BusinessException(ErrorCode.SEGREGATION_OF_DUTIES,
                    "User " + approver + " membuat jurnal " + journalNumber + " sehingga tidak boleh menyetujuinya");
        }
        assertBalanced();
        this.status = next;
        this.approvedBy = approver;
        this.approvedAt = now;
    }

    /** APPROVED -> DRAFT: dikembalikan ke pembuat untuk revisi. */
    public void returnToDraft() {
        this.status = status.transitionTo(JournalStatus.DRAFT);
        this.approvedBy = null;
        this.approvedAt = null;
    }

    /** APPROVED -> POSTED. Validasi posting rule dilakukan oleh PostingService. */
    public void markPosted(String postedBy, Instant now) {
        this.status = status.transitionTo(JournalStatus.POSTED);
        this.postedBy = postedBy;
        this.postedAt = now;
    }

    /** POSTED -> REVERSED. Isi jurnal asli tetap utuh. */
    public void markReversed(String reversedBy, Instant now) {
        this.status = status.transitionTo(JournalStatus.REVERSED);
        this.reversedBy = reversedBy;
        this.reversedAt = now;
    }

    /** Jurnal pembalik dibuat sistem atas perintah user berwenang: DRAFT -> APPROVED -> POSTED. */
    public void approveAndPost(String actor, Instant now) {
        assertBalanced();
        this.status = status.transitionTo(JournalStatus.APPROVED);
        this.approvedBy = actor;
        this.approvedAt = now;
        markPosted(actor, now);
    }

    public void linkReversalOf(JournalEntry original, String reason) {
        this.reversalOf = original;
        this.reversalReason = reason;
    }

    /** Ringkasan state untuk before/after di audit event. */
    public Map<String, Object> snapshot() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("journalNumber", journalNumber);
        s.put("journalDate", journalDate);
        s.put("journalType", journalType);
        s.put("status", status);
        s.put("description", description);
        s.put("autoReverse", autoReverse);
        s.put("totalDebit", totalDebit());
        s.put("totalCredit", totalCredit());
        s.put("lineCount", lines.size());
        s.put("version", version);
        return s;
    }
}
