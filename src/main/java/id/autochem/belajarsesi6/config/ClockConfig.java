package id.autochem.belajarsesi6.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

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
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    /** Clock di-inject agar aturan tanggal (future-dated, cut-off) mudah di-test. */
    @Bean
    Clock clock(@Value("${app.zone:Asia/Jakarta}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
