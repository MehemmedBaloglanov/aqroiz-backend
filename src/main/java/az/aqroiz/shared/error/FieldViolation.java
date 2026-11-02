package az.aqroiz.shared.error;

/**
 * Validasiya xətasında bir sahə üzrə səhv.
 */
public record FieldViolation(String field, String message) {
}
