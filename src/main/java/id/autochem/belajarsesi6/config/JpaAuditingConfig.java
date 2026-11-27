package id.autochem.belajarsesi6.config;

import id.autochem.belajarsesi6.security.CurrentUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;
import java.util.Optional;

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
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

    /** @CreatedBy / @LastModifiedBy = username dari SecurityContext (fallback "system"). */
    @Bean
    AuditorAware<String> auditorAware() {
        return () -> Optional.of(CurrentUser.username());
    }

    /** @CreatedDate / @LastModifiedDate memakai Clock yang sama dengan aturan bisnis (mudah di-test). */
    @Bean
    DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(clock.instant());
    }
}
