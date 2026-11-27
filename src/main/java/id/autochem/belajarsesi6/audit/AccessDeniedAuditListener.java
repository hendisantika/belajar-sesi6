package id.autochem.belajarsesi6.audit;

import id.autochem.belajarsesi6.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
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
/**
 * Mencatat percobaan aksi oleh user yang tidak berwenang (mis. ACCOUNTANT mencoba lock periode).
 * Request anonim tidak dicatat agar audit log tidak dibanjiri noise.
 */
@Component
@RequiredArgsConstructor
class AccessDeniedAuditListener {

    private final AuditTrail audit;

    @EventListener
    void onDenied(AuthorizationDeniedEvent<?> event) {
        Authentication auth = event.getAuthentication().get();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return;
        }
        String target = describeTarget(event.getSource());
        audit.denied(auth.getName(), CurrentUser.roles(auth), target,
                "Akses ditolak untuk " + auth.getName() + " (" + CurrentUser.roles(auth) + ") ke " + target);
    }

    private static String describeTarget(Object source) {
        HttpServletRequest request = AuditTrail.currentRequest();
        String http = request != null ? request.getMethod() + " " + request.getRequestURI() : null;
        if (source instanceof MethodInvocation mi) {
            String method = mi.getMethod().getDeclaringClass().getSimpleName() + "." + mi.getMethod().getName();
            return http != null ? http + " [" + method + "]" : method;
        }
        return http != null ? http : String.valueOf(source);
    }
}
