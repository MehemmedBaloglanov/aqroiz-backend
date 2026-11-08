package az.aqroiz.identity.internal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import az.aqroiz.identity.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Token yaradılması və yoxlanması. Spring konteksti qaldırılmır:
 * JwtConfig-in metodları birbaşa çağırılır, vaxt sabit Clock ilə idarə olunur.
 */
class JwtTokenTests {

    private static final Instant NOW = Instant.parse("2026-11-09T08:00:00Z");
    private static final String SECRET = "test-secret-0123456789-0123456789-abcdef";

    private final JwtConfig config = new JwtConfig();
    private final JwtProperties properties = new JwtProperties(SECRET, Duration.ofHours(12), "aqroiz");
    private final SecretKey key = config.jwtSigningKey(properties);
    private final TokenService tokenService = new TokenService(config.jwtEncoder(key), properties, clockAt(NOW));
    private final UserAccount user = UserAccount.create(UUID.randomUUID(), "kenan@mail.az", "Kənan Həsənov",
            null, "{bcrypt}hash", Role.MANAGER, NOW);

    @Test
    void issuedTokenContainsUserTenantAndRole() {
        IssuedToken token = tokenService.issue(user);

        Jwt jwt = decoderAt(NOW.plusSeconds(60)).decode(token.value());

        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getClaimAsString("tenantId")).isEqualTo(user.getTenantId().toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("MANAGER");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("aqroiz");
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(12)));
        assertThat(token.expiresAt()).isEqualTo(jwt.getExpiresAt());
    }

    @Test
    void expiredTokenIsRejected() {
        IssuedToken token = tokenService.issue(user);

        JwtDecoder thirteenHoursLater = decoderAt(NOW.plus(Duration.ofHours(13)));

        assertThatThrownBy(() -> thirteenHoursLater.decode(token.value()))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        IssuedToken token = tokenService.issue(user);

        JwtProperties otherProperties = new JwtProperties(
                "another-secret-0123456789-0123456789-xyz", Duration.ofHours(12), "aqroiz");
        JwtDecoder otherDecoder = config.jwtDecoder(
                config.jwtSigningKey(otherProperties), otherProperties, clockAt(NOW));

        assertThatThrownBy(() -> otherDecoder.decode(token.value()))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void passwordEncoderSaltsHashesAndMatches() {
        PasswordEncoder encoder = config.passwordEncoder();

        String first = encoder.encode("Gizli-Şifrə-123");
        String second = encoder.encode("Gizli-Şifrə-123");

        assertThat(first).startsWith("{").isNotEqualTo(second);
        assertThat(first.length()).isLessThanOrEqualTo(255);
        assertThat(encoder.matches("Gizli-Şifrə-123", first)).isTrue();
        assertThat(encoder.matches("yanlış-şifrə", first)).isFalse();
    }

    private JwtDecoder decoderAt(Instant instant) {
        return config.jwtDecoder(key, properties, clockAt(instant));
    }

    private static Clock clockAt(Instant instant) {
        return Clock.fixed(instant, ZoneId.of("Asia/Baku"));
    }
}
