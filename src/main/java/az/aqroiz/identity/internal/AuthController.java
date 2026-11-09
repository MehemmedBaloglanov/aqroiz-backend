package az.aqroiz.identity.internal;

import az.aqroiz.identity.Role;
import az.aqroiz.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

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
    /** Tokenə görə cari istifadəçi. */
    @GetMapping("/me")
    MeResponse me(CurrentUser currentUser) {
        AuthService.Account account = authService.currentAccount(currentUser.userId());
        return toMe(account.user(), account.tenant());
    }

    private static MeResponse toMe(UserAccount user, Tenant tenant) {
        return new MeResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                new TenantSummary(tenant.getId(), tenant.getName()));
    }
}
