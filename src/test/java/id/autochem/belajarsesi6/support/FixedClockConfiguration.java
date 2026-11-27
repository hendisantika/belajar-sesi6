package id.autochem.belajarsesi6.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Created by IntelliJ IDEA.
 * Project : belajar-sesi6
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Link : s.id/hendisantika
 * Date: 27/11/26
 * Time: 19.56
 */
@TestConfiguration(proxyBeanMethods = false)
public class FixedClockConfiguration {

    /** "Hari ini" = 3 Des 2026: November sudah berakhir sehingga bisa di-cut-off & di-lock. */
    public static final Instant NOW = Instant.parse("2026-12-03T03:00:00Z");

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(NOW, ZoneId.of("Asia/Jakarta"));
    }
}
