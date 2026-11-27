package id.autochem.belajarsesi6.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
public class OpenApiConfig {

    @Bean
    OpenAPI sesi6OpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Trial Balance, Period Closing & Audit Trail API")
                        .description("Sesi 6 - neraca saldo dari GL, cut-off & lock periode, adjustment/reversal, "
                                + "audit trail terstruktur, dan rekonsiliasi Journal - GL - Trial Balance")
                        .version("v1")
                        .contact(new Contact().name("hendisantika").url("https://s.id/hendisantika")))
                .components(new Components().addSecuritySchemes("basicAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }
}
