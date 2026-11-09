package az.aqroiz.shared.error;

/**
 * Bütün modullar üçün ümumi xəta kodları.
 * Biznes qaydalarının kodları (məsələn, NET_NOT_POSITIVE) öz modullarında saxlanılır.
 */

public final class ErrorCodes {

    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String MALFORMED_REQUEST = "MALFORMED_REQUEST";
    public static final String TYPE_MISMATCH = "TYPE_MISMATCH";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String TOKEN_INVALID = "TOKEN_INVALID";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
    public static final String TENANT_SUSPENDED = "TENANT_SUSPENDED";

    private ErrorCodes() {
    }
}
