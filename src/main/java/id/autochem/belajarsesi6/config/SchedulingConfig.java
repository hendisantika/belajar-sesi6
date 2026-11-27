package id.autochem.belajarsesi6.config;

import id.autochem.belajarsesi6.idempotency.IdempotencyProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

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
@EnableScheduling
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IdempotencyProperties.class)
public class SchedulingConfig {
}
