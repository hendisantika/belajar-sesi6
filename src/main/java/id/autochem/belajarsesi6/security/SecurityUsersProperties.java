package id.autochem.belajarsesi6.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

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
@ConfigurationProperties("app.security")
public record SecurityUsersProperties(@DefaultValue List<User> users) {

    public record User(String username, String password, List<String> roles) {
    }
}
