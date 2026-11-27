package id.autochem.belajarsesi6.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.stream.Collectors;

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
public final class CurrentUser {

    /** Actor untuk proses tanpa user login (mis. scheduler / migrasi). */
    public static final String SYSTEM = "system";

    private CurrentUser() {
    }

    public static Optional<Authentication> authentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.of(auth);
    }

    public static String username() {
        return authentication().map(Authentication::getName).orElse(SYSTEM);
    }

    /** Role user tanpa prefix ROLE_, dipisah koma (mis. "ADMIN"). */
    public static String roles() {
        return authentication().map(CurrentUser::roles).orElse(null);
    }

    public static String roles(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a != null && a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .sorted()
                .collect(Collectors.joining(","));
    }
}
