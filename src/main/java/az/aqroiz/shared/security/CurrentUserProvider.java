package az.aqroiz.shared.security;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Cari istifadəçini Spring Security kontekstindən (yoxlanmış JWT-dən) oxuyur.
 */
@Component
public class CurrentUserProvider {

    /**
     * @throws AuthenticationCredentialsNotFoundException sorğu tokensizdir (cavab: 401)
     */
    public CurrentUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new AuthenticationCredentialsNotFoundException("Sistemə daxil olunmayıb.");
        }
        Jwt jwt = jwtAuthentication.getToken();
        return new CurrentUser(
                UUID.fromString(jwt.getSubject()),
                UUID.fromString(jwt.getClaimAsString(JwtClaims.TENANT_ID)),
                jwt.getClaimAsString(JwtClaims.ROLE));
    }
}
