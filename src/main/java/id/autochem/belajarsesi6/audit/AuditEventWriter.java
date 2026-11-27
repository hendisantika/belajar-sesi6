package id.autochem.belajarsesi6.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

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
@Slf4j
@Component
@RequiredArgsConstructor
class AuditEventWriter {

    private final AuditEventRepository events;
    private final ObjectMapper objectMapper;

    /**
     * Ditulis tepat sebelum commit di transaksi bisnis yang sama:
     * bisnis commit -> audit ada; bisnis rollback -> audit ikut hilang (tidak ada jejak palsu).
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    void onCommitted(AuditRecord.Committed event) {
        events.save(toEntity(event.record()));
    }

    /**
     * Penolakan justru terjadi saat transaksi bisnis rollback, sehingga dicatat
     * setelah rollback di transaksi baru agar tetap tersimpan.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void onRejected(AuditRecord.Rejected event) {
        AuditRecord r = event.record();
        log.info("Audit {} {} {}#{} by {}: {}", r.outcome(), r.action(), r.entityType(),
                r.entityRef() != null ? r.entityRef() : r.entityId(), r.actor(), r.reason());
        events.save(toEntity(r));
    }

    private AuditEvent toEntity(AuditRecord r) {
        return AuditEvent.from(r, json(r.before()), json(r.after()));
    }

    private String json(Object value) {
        return value == null ? null : objectMapper.writeValueAsString(value);
    }
}
