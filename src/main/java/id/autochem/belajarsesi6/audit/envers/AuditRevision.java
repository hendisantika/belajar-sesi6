package id.autochem.belajarsesi6.audit.envers;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

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
/** Satu revisi Envers per transaksi yang mengubah entity @Audited, lengkap dengan username. */
@Getter
@Entity
@Table(name = "revinfo")
@RevisionEntity(AuditRevisionListener.class)
public class AuditRevision {

    @Id
    @RevisionNumber
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @RevisionTimestamp
    @Column(name = "revtstmp", nullable = false)
    private long timestamp;

    @Setter
    @Column(length = 100)
    private String username;
}
