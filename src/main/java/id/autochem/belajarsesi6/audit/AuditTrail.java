package id.autochem.belajarsesi6.audit;

import id.autochem.belajarsesi6.common.exception.BusinessException;
import id.autochem.belajarsesi6.common.web.CorrelationIdFilter;
import id.autochem.belajarsesi6.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
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
 * Pintu masuk pencatatan audit. Konteks (actor, role, correlation id, IP, waktu) diambil
 * saat event dipublikasikan, lalu ditulis oleh {@link AuditEventWriter} lewat
 * {@code @TransactionalEventListener} agar konsisten dengan transaksi bisnis.
 */
@Component
@RequiredArgsConstructor
public class AuditTrail {

    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    public void success(AuditAction action, String entityType, Long entityId, String entityRef,
                        Object before, Object after) {
        success(action, entityType, entityId, entityRef, null, before, after);
    }

    /** Aksi sukses beserta alasan (wajib untuk aksi sensitif seperti unlock periode). */
    public void success(AuditAction action, String entityType, Long entityId, String entityRef, String reason,
                        Object before, Object after) {
        publisher.publishEvent(new AuditRecord.Committed(record(action, AuditOutcome.SUCCESS, entityType, entityId,
                entityRef, CurrentUser.username(), CurrentUser.roles(), reason, null, before, after)));
    }

    public void rejected(AuditAction action, String entityType, Long entityId, String entityRef,
                         String reason, String errorCode) {
        publisher.publishEvent(new AuditRecord.Rejected(record(action, AuditOutcome.REJECTED, entityType, entityId,
                entityRef, CurrentUser.username(), CurrentUser.roles(), reason, errorCode, null, null)));
    }

    public void rejected(AuditAction action, String entityType, Long entityId, String entityRef,
                         BusinessException ex) {
        rejected(action, entityType, entityId, entityRef, ex.getMessage(), ex.getCode().name());
    }

    public void denied(String actor, String roles, String target, String reason) {
        publisher.publishEvent(new AuditRecord.Rejected(record(AuditAction.ACCESS_DENIED, AuditOutcome.DENIED,
                EntityTypes.ENDPOINT, null, target, actor, roles, reason, "ACCESS_DENIED", null, null)));
    }

    private AuditRecord record(AuditAction action, AuditOutcome outcome, String entityType, Long entityId,
                               String entityRef, String actor, String roles, String reason, String errorCode,
                               Object before, Object after) {
        HttpServletRequest request = currentRequest();
        String correlationId = request != null ? (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE) : null;
        String clientIp = request != null ? request.getRemoteAddr() : null;
        return new AuditRecord(UUID.randomUUID(), action, outcome, entityType, entityId, entityRef, actor, roles,
                reason, errorCode, before, after, correlationId, clientIp, clock.instant());
    }

    static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }
}
