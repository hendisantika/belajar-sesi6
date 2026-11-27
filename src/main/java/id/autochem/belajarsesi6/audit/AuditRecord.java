package id.autochem.belajarsesi6.audit;

import java.time.Instant;
import java.util.UUID;

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
/**
 * Audit event terstruktur: siapa (actor), kapan (occurredAt), apa (action + outcome),
 * referensi (entityType/entityId/entityRef), dan before/after untuk perubahan data.
 */
public record AuditRecord(UUID eventId,
                          AuditAction action,
                          AuditOutcome outcome,
                          String entityType,
                          Long entityId,
                          String entityRef,
                          String actor,
                          String actorRoles,
                          String reason,
                          String errorCode,
                          Object before,
                          Object after,
                          String correlationId,
                          String clientIp,
                          Instant occurredAt) {

    /** Dipublikasikan untuk aksi sukses; ditulis BEFORE_COMMIT di transaksi bisnis yang sama. */
    public record Committed(AuditRecord record) {
    }

    /** Dipublikasikan untuk aksi ditolak; ditulis AFTER_ROLLBACK di transaksi baru. */
    public record Rejected(AuditRecord record) {
    }
}
