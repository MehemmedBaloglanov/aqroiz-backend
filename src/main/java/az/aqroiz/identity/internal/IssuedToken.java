package az.aqroiz.identity.internal;
import java.time.Instant;

/**
 * Verilmiş giriş tokeni.
 *
 * @param value     JWT mətni (Authorization: Bearer ... başlığında göndərilir)
 * @param expiresAt tokenin bitmə vaxtı (frontend vaxtında yenidən girişə yönləndirsin)
 */
public record IssuedToken(String value, Instant expiresAt) {
}