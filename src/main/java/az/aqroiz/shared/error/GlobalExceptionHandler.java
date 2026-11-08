package az.aqroiz.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Bütün API xətalarını vahid formata (RFC 9457 ProblemDetail + code + timestamp) çevirir.
 * <p>
 * ResponseEntityExceptionHandler-dən miras alırıq ki, Spring MVC-nin öz xətaları
 * (405, 415, oxunmayan JSON və s.) də eyni formatda qaytarılsın.
 */

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock){
        this.clock = clock;
    }

    // ---------- Bizim istisnalar ----------

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex, HttpServletRequest request){
        return problem(HttpStatus.NOT_FOUND,"Tapılmadı",ex.getMessage(),ErrorCodes.NOT_FOUND,request.getRequestURI());
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Biznes qaydası pozuldu", ex.getMessage(), ex.getCode(),
                request.getRequestURI());
    }

    // ---------- Sorğu parametrləri ----------

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String detail = "“" + ex.getName() + "” parametrinin dəyəri düzgün formatda deyil.";
        return problem(HttpStatus.BAD_REQUEST, "Yanlış sorğu", detail, ErrorCodes.TYPE_MISMATCH, request.getRequestURI());
    }

    // ---------- Təhlükəsizlik ----------
    // Aşağıdakı “hər şeyi tutan” handler bunları 500-ə çevirməsin deyə ayrıca tuturuq.

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "İcazə yoxdur", "Bu əməliyyat sizin rolunuz üçün icazəli deyil.",
                ErrorCodes.FORBIDDEN, request.getRequestURI());
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "Giriş tələb olunur", "Sistemə daxil olun.",
                ErrorCodes.UNAUTHORIZED, request.getRequestURI());
    }

    // ---------- Gözlənilməz xətalar ----------

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Gözlənilməz xəta: {} {}", request.getMethod(), request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Daxili xəta",
                "Gözlənilməz xəta baş verdi. Bir az sonra yenidən cəhd edin.",
                ErrorCodes.INTERNAL_ERROR, request.getRequestURI());
    }

    // ---------- Spring MVC xətalarının üzərinə yazılanlar ----------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request
                                                                  ){
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST,"Yalnış sorğu","Bəzi sahələr düzgün doldurulmayıb.",
                ErrorCodes.VALIDATION_FAILED,path(request));

        List<FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(),error.getDefaultMessage()))
                .toList();
        body.setProperty("errors",errors);
        return handleExceptionInternal(ex,body,headers,status,request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Yanlış sorğu",
                "Sorğunun gövdəsi oxunmur. JSON formatını yoxlayın.", ErrorCodes.MALFORMED_REQUEST, path(request));
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * Spring MVC-nin qalan bütün xətaları (405, 415 və s.) buradan keçir.
     * Onlara da code, timestamp və instance əlavə edirik.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            addMissingFields(problem, statusCode, request);
        }
        return response;
    }

    // ---------- Köməkçi metodlar ----------

    private void addMissingFields(ProblemDetail problem, HttpStatusCode status, WebRequest request) {
        Map<String, Object> properties = problem.getProperties();
        boolean hasCode = properties != null && properties.containsKey("code");
        if (!hasCode) {
            problem.setProperty("code", "HTTP_" + status.value());
            problem.setTitle(titleFor(status, problem.getTitle()));
        }
        if (properties == null || !properties.containsKey("timestamp")) {
            problem.setProperty("timestamp", clock.instant().toString());
        }
        if (problem.getInstance() == null) {
            problem.setInstance(URI.create(path(request)));
        }
    }

    private static String titleFor(HttpStatusCode status, String fallback){
        return switch (status.value()){
            case 400 -> "Yanlış sorğu";
            case 404 -> "Tapılmadı";
            case 405 -> "Metod dəstəklənmir";
            case 406 -> "Cavab formatı dəstəklənmir";
            case 415 -> "Məzmun növü dəstəklənmir";
            default -> fallback;
        };
    }
    private ProblemDetail problem(HttpStatus status, String title, String detail, String code, String path){
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status,detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(path));
        problem.setProperty("code",code);
        problem.setProperty("timestamp", clock.instant().toString());
        return problem;
    }

    private static String path(WebRequest request){
        if (request instanceof ServletWebRequest servletRequest){
            return servletRequest.getRequest().getRequestURI();
        }
        return "/";
    }
}
