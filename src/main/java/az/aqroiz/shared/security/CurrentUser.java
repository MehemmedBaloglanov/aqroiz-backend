package az.aqroiz.shared.security;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Sorğunu göndərən istifadəçi. JWT-dən oxunur.
 * <p>
 * QAYDA: tenantId yalnız buradan götürülür — heç vaxt URL-dən, sorğu parametrindən və ya JSON-dan.
 *
 * @param userId   istifadəçinin ID-si (JWT sub)
 * @param tenantId şirkətin ID-si
 * @param role     rol adı (OWNER, MANAGER, ...)
 */
public record CurrentUser(UUID userId, UUID tenantId, String role) {

    public CurrentUser {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(role, "role");
    }

    public boolean hasRole(String expectedRole) {
        return role.equals(expectedRole);
    }

    public boolean hasAnyRole(String... expectedRoles) {
        return Arrays.asList(expectedRoles).contains(role);
    }
}
