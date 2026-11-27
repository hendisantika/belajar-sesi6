package id.autochem.belajarsesi6.audit;

import id.autochem.belajarsesi6.audit.dto.AuditEventResponse;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
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
 * Time: 19.52
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditQueryService {

    private final AuditEventRepository events;
    private final ObjectMapper objectMapper;

    public record Filter(AuditAction action, Set<AuditOutcome> outcomes, String entityType, Long entityId,
                         String entityRef, String actor, Instant from, Instant to) {
    }

    public Page<AuditEventResponse> search(Filter filter, Pageable pageable) {
        return events.findAll(spec(filter), pageable).map(this::toResponse);
    }

    public List<AuditEventResponse> trail(String entityType, Long entityId) {
        return events.findByEntityTypeAndEntityIdOrderByOccurredAtAscIdAsc(entityType, entityId).stream()
                .map(this::toResponse)
                .toList();
    }

    private Specification<AuditEvent> spec(Filter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.action() != null) {
                p.add(cb.equal(root.get("action"), f.action()));
            }
            if (f.outcomes() != null && !f.outcomes().isEmpty()) {
                p.add(root.get("outcome").in(f.outcomes()));
            }
            if (f.entityType() != null) {
                p.add(cb.equal(root.get("entityType"), f.entityType()));
            }
            if (f.entityId() != null) {
                p.add(cb.equal(root.get("entityId"), f.entityId()));
            }
            if (f.entityRef() != null) {
                p.add(cb.equal(root.get("entityRef"), f.entityRef()));
            }
            if (f.actor() != null) {
                p.add(cb.equal(root.get("actor"), f.actor()));
            }
            if (f.from() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), f.from()));
            }
            if (f.to() != null) {
                p.add(cb.lessThan(root.get("occurredAt"), f.to()));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private AuditEventResponse toResponse(AuditEvent e) {
        return new AuditEventResponse(e.getId(), e.getEventId(), e.getOccurredAt(), e.getActor(), e.getActorRoles(),
                e.getAction(), e.getOutcome(), e.getEntityType(), e.getEntityId(), e.getEntityRef(), e.getReason(),
                e.getErrorCode(), json(e.getBeforeState()), json(e.getAfterState()), e.getCorrelationId(),
                e.getClientIp());
    }

    private JsonNode json(String value) {
        return value == null ? null : objectMapper.readTree(value);
    }
}
