package id.autochem.belajarsesi6.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
public class IdempotencyCleanupJob {

    private final IdempotencyService service;

    /** Key memiliki masa berlaku (default 24 jam); key kedaluwarsa dibersihkan berkala. */
    @Scheduled(cron = "${app.idempotency.cleanup-cron:0 0 * * * *}")
    public void purgeExpiredKeys() {
        int deleted = service.purgeExpired();
        if (deleted > 0) {
            log.info("Purged {} expired idempotency keys", deleted);
        }
    }
}
