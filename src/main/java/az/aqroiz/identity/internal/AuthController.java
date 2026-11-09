package az.aqroiz.identity.internal;

import az.aqroiz.identity.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank String password) {
    }

    record TenantSummary(UUID id, String name) {
    }

    record MeResponse(UUID id, String email, String fullName, Role role, TenantSummary tenant) {
    }

    record LoginResponse(String accessToken, String tokenType, Instant expiresAt, MeResponse user) {
    }

    private final AuthService authService;

    AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Giriş. Açıq endpoint (SecurityConfig). */
    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.email(), request.password());
        return new LoginResponse(
                result.token().value(),
                "Bearer",
                result.token().expiresAt(),
                toMe(result.user(), result.tenant()));
    }

    /** Tokenə görə cari istifadəçi. */
    @GetMapping("/me")
    MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        AuthService.Account account = authService.currentAccount(UUID.fromString(jwt.getSubject()));
        return toMe(account.user(), account.tenant());
    }

    private static MeResponse toMe(UserAccount user, Tenant tenant) {
        return new MeResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                new TenantSummary(tenant.getId(), tenant.getName()));
    }
}
