package az.aqroiz.identity.internal;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import az.aqroiz.shared.security.JwtClaims;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * İstifadəçi üçün JWT giriş tokeni yaradır.
 * <p>
 * Tokenə yalnız identifikatorlar və rol yazılır. Şifrə, e-poçt kimi məlumatlar yazılmır:
 * tokenin məzmunu şifrələnmir, hər kəs onu oxuya bilər.
 */
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    public TokenService(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedToken issue(UserAccount user) {
        // JWT vaxtı saniyə dəqiqliyində saxlayır; cavabdakı expiresAt tokendəki ilə eyni olsun deyə kəsirik.
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = now.plus(properties.ttl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(JwtClaims.TENANT_ID, user.getTenantId().toString())
                .claim(JwtClaims.ROLE, user.getRole().name())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(value, expiresAt);
    }
}
