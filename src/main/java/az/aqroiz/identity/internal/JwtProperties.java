package az.aqroiz.identity.internal;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * JWT parametrləri (application.yaml → aqroiz.security.jwt).
 *
 * @param secret imza açarı, ən azı 32 bayt (HS256 üçün 256 bit tələb olunur)
 * @param ttl    tokenin ömrü
 * @param issuer tokeni verən sistemin adı (iss claim)
 */
@ConfigurationProperties(prefix = "aqroiz.security.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("PT12H") Duration ttl,
        @DefaultValue("aqroiz") String issuer) {

    static final int MIN_SECRET_BYTES = 32;

    /** Səhv konfiqurasiyada tətbiq ümumiyyətlə işə düşmür. */
    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "aqroiz.security.jwt.secret ən azı " + MIN_SECRET_BYTES + " bayt olmalıdır");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("aqroiz.security.jwt.ttl müsbət olmalıdır");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("aqroiz.security.jwt.issuer boş ola bilməz");
        }
    }
}
