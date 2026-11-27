package id.autochem.belajarsesi6.audit.dto;

import id.autochem.belajarsesi6.audit.AuditAction;
import id.autochem.belajarsesi6.audit.AuditOutcome;
import tools.jackson.databind.JsonNode;

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
 * Time: 19.52
 */
public record AuditEventResponse(Long id, UUID eventId, Instant occurredAt, String actor, String actorRoles,
                                 AuditAction action, AuditOutcome outcome, String entityType, Long entityId,
                                 String entityRef, String reason, String errorCode, JsonNode before,
                                 JsonNode after, String correlationId, String clientIp) {
}
