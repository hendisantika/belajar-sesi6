package id.autochem.belajarsesi6.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

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
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long>, JpaSpecificationExecutor<AuditEvent> {

    List<AuditEvent> findByEntityTypeAndEntityIdOrderByOccurredAtAscIdAsc(String entityType, Long entityId);
}
