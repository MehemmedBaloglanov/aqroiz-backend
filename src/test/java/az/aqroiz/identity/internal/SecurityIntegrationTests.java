package az.aqroiz.identity.internal;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import az.aqroiz.TestcontainersConfiguration;
import az.aqroiz.identity.Role;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Təhlükəsizlik qaydalarını bütün tətbiq üzərində yoxlayır: real filtrlər, real JWT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, SecurityIntegrationTests.TestController.class})
class SecurityIntegrationTests {

    private static final Instant NOW = Instant.parse("2026-11-09T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void requestWithoutTokenReturns401InOurFormat() throws Exception {
        mockMvc.perform(get("/test/secure/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void invalidTokenReturns401WithTokenInvalidCode() throws Exception {
        mockMvc.perform(get("/test/secure/me").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    void validTokenIdentifiesUserAndRole() throws Exception {
        UserAccount user = user(Role.MANAGER);

        mockMvc.perform(get("/test/secure/me").header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(user.getId().toString()))
                .andExpect(jsonPath("$.authorities", hasItem("ROLE_MANAGER")));
    }

    @Test
    void ownerOnlyEndpointRejectsManagerWith403() throws Exception {
        mockMvc.perform(get("/test/secure/owner-only").header(HttpHeaders.AUTHORIZATION, bearer(user(Role.MANAGER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void ownerOnlyEndpointAllowsOwner() throws Exception {
        mockMvc.perform(get("/test/secure/owner-only").header(HttpHeaders.AUTHORIZATION, bearer(user(Role.OWNER))))
                .andExpect(status().isOk());
    }

    @Test
    void postWorksWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/test/secure/echo").header(HttpHeaders.AUTHORIZATION, bearer(user(Role.RECEIVER))))
                .andExpect(status().isOk());
    }

    private static UserAccount user(Role role) {
        return UserAccount.create(UUID.randomUUID(), "test@aqroiz.az", "Test İstifadəçi", null,
                "{noop}not-used", role, NOW);
    }

    private String bearer(UserAccount user) {
        return "Bearer " + tokenService.issue(user).value();
    }

    @RestController
    @RequestMapping("/test/secure")
    static class TestController {

        @GetMapping("/me")
        Map<String, Object> me(Authentication authentication) {
            return Map.of(
                    "name", authentication.getName(),
                    "authorities", authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .toList());
        }

        @GetMapping("/owner-only")
        @PreAuthorize("hasRole('OWNER')")
        String ownerOnly() {
            return "ok";
        }

        @PostMapping("/echo")
        String echo() {
            return "ok";
        }
    }
}
