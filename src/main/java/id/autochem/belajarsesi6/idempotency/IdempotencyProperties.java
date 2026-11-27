package id.autochem.belajarsesi6.idempotency;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

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
@ConfigurationProperties("app.idempotency")
public record IdempotencyProperties(@DefaultValue("24h") Duration ttl,
                                    @DefaultValue("0 0 * * * *") String cleanupCron) {
}
