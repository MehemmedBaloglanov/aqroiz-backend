package az.aqroiz.shared.security;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class CurrentUserProviderTests {

    private final CurrentUserProvider provider = new CurrentUserProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext(); // növbəti testə “giriş edilmiş istifadəçi” qalmasın
    }

    @Test
    void readsUserTenantAndRoleFromJwt() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(userId.toString())
                .claim(JwtClaims.TENANT_ID, tenantId.toString())
                .claim(JwtClaims.ROLE, "MANAGER")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        CurrentUser currentUser = provider.get();

        assertThat(currentUser.userId()).isEqualTo(userId);
        assertThat(currentUser.tenantId()).isEqualTo(tenantId);
        assertThat(currentUser.role()).isEqualTo("MANAGER");
    }

    @Test
    void missingAuthenticationIsRejected() {
        assertThatThrownBy(provider::get).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void roleChecks() {
        CurrentUser manager = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), "MANAGER");

        assertThat(manager.hasRole("MANAGER")).isTrue();
        assertThat(manager.hasRole("OWNER")).isFalse();
        assertThat(manager.hasAnyRole("OWNER", "MANAGER")).isTrue();
        assertThat(manager.hasAnyRole("OWNER", "ACCOUNTANT")).isFalse();
    }
}
