package id.autochem.belajarsesi6.period;

import id.autochem.belajarsesi6.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

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
@Table(name = "accounting_period")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountingPeriod extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 7)
    private String code;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private PeriodStatus status;

    @Column(name = "cutoff_by", length = 100)
    private String cutoffBy;

    @Column(name = "cutoff_at")
    private Instant cutoffAt;

    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "unlocked_by", length = 100)
    private String unlockedBy;

    @Column(name = "unlocked_at")
    private Instant unlockedAt;

    @Column(name = "unlock_reason", length = 500)
    private String unlockReason;

    @Version
    private Long version;

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /** OPEN -> SOFT_CLOSED: transaksi harian ditutup, jurnal penyesuaian masih boleh. */
    public void cutOff(String actor, Instant now) {
        this.status = status.transitionTo(PeriodStatus.SOFT_CLOSED);
        this.cutoffBy = actor;
        this.cutoffAt = now;
    }

    /** SOFT_CLOSED -> OPEN: batal cut-off (alasan dicatat di audit). */
    public void reopen() {
        this.status = status.transitionTo(PeriodStatus.OPEN);
        this.cutoffBy = null;
        this.cutoffAt = null;
    }

    /** SOFT_CLOSED -> LOCKED: dipanggil setelah closing checklist lolos. */
    public void lock(String actor, Instant now) {
        this.status = status.transitionTo(PeriodStatus.LOCKED);
        this.lockedBy = actor;
        this.lockedAt = now;
    }

    /** LOCKED -> SOFT_CLOSED: hanya koreksi (adjustment/reversal) yang boleh masuk sampai di-lock lagi. */
    public void unlock(String actor, String reason, Instant now) {
        this.status = status.transitionTo(PeriodStatus.SOFT_CLOSED);
        this.unlockedBy = actor;
        this.unlockedAt = now;
        this.unlockReason = reason;
        this.lockedBy = null;
        this.lockedAt = null;
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("code", code);
        s.put("status", status);
        s.put("cutoffBy", cutoffBy);
        s.put("lockedBy", lockedBy);
        s.put("lockedAt", lockedAt);
        s.put("unlockedBy", unlockedBy);
        s.put("unlockReason", unlockReason);
        s.put("version", version);
        return s;
    }
}
