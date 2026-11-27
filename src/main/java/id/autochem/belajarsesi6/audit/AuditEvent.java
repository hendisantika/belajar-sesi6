package id.autochem.belajarsesi6.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Getter
@Entity
@Immutable
@Table(name = "audit_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, updatable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60, updatable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private AuditOutcome outcome;

    @Column(name = "entity_type", nullable = false, length = 50, updatable = false)
    private String entityType;

    @Column(name = "entity_id", updatable = false)
    private Long entityId;

    @Column(name = "entity_ref", length = 100, updatable = false)
    private String entityRef;

    @Column(nullable = false, length = 100, updatable = false)
    private String actor;

    @Column(name = "actor_roles", length = 200, updatable = false)
    private String actorRoles;

    @Column(length = 500, updatable = false)
    private String reason;

    @Column(name = "error_code", length = 60, updatable = false)
    private String errorCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_state", updatable = false)
    private String beforeState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state", updatable = false)
    private String afterState;

    @Column(name = "correlation_id", length = 64, updatable = false)
    private String correlationId;

    @Column(name = "client_ip", length = 64, updatable = false)
    private String clientIp;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    static AuditEvent from(AuditRecord r, String beforeJson, String afterJson) {
        AuditEvent e = new AuditEvent();
        e.eventId = r.eventId();
        e.action = r.action();
        e.outcome = r.outcome();
        e.entityType = r.entityType();
        e.entityId = r.entityId();
        e.entityRef = truncate(r.entityRef(), 100);
        e.actor = r.actor();
        e.actorRoles = truncate(r.actorRoles(), 200);
        e.reason = truncate(r.reason(), 500);
        e.errorCode = r.errorCode();
        e.beforeState = beforeJson;
        e.afterState = afterJson;
        e.correlationId = r.correlationId();
        e.clientIp = r.clientIp();
        e.occurredAt = r.occurredAt();
        return e;
    }

    private static String truncate(String value, int max) {
        return value != null && value.length() > max ? value.substring(0, max) : value;
    }
}
