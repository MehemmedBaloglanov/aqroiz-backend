package az.aqroiz.identity.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Spring Security filtrlərində yaranan 401 və 403 xətalarını Spring MVC-nin xəta mexanizminə ötürür.
 * Nəticədə onları da GlobalExceptionHandler emal edir və cavab vahid ProblemDetail formatında olur.
 */
@Component
class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final HandlerExceptionResolver resolver;

    ProblemDetailSecurityHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    /** 401: token yoxdur və ya etibarsızdır. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) {
        // HTTP standartı: 401 cavabında hansı giriş üsulunun gözlənildiyi göstərilir.
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        resolver.resolveException(request, response, null, exception);
    }

    /** 403: token düzgündür, amma rolun icazəsi yoxdur. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) {
        resolver.resolveException(request, response, null, exception);
    }
}
