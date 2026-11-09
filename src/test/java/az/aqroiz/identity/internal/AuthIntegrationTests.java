package az.aqroiz.identity.internal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import az.aqroiz.TestcontainersConfiguration;
import az.aqroiz.identity.Role;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Giriş və /me endpoint-ləri: real HTTP qatı, real təhlükəsizlik, real PostgreSQL.
 * Hər test tranzaksiyada işləyir və sonda geri qaytarılır.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthIntegrationTests {

    private static final String EMAIL = "rehber@test.az";
    private static final String PASSWORD = "Dogru-Sifre-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantRepository tenants;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    private Tenant tenant;
    private UserAccount owner;

    @BeforeEach
    void createTenantAndUser() {
        Instant now = clock.instant();
        tenant = tenants.save(Tenant.register("Test Şirkəti", null, now));
        owner = users.save(UserAccount.create(tenant.getId(), EMAIL, "Test Rəhbər", null,
                passwordEncoder.encode(PASSWORD), Role.OWNER, now));
    }

    @Test
    void loginReturnsTokenAndUser() throws Exception {
        login(EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andExpect(jsonPath("$.user.role").value("OWNER"))
                .andExpect(jsonPath("$.user.tenant.name").value("Test Şirkəti"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void loginIgnoresEmailCase() throws Exception {
        login("REHBER@Test.AZ", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void loginRecordsLastLoginTime() throws Exception {
        login(EMAIL, PASSWORD).andExpect(status().isOk());

        assertThat(users.findById(owner.getId()).orElseThrow().getLastLoginAt()).isNotNull();
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        login(EMAIL, "Yanlis-Sifre-1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value("E-poçt və ya şifrə yanlışdır."));
    }

    @Test
    void unknownEmailGetsExactlyTheSameAnswerAsWrongPassword() throws Exception {
        login("yoxdur@test.az", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value("E-poçt və ya şifrə yanlışdır."));
    }

    @Test
    void tooLongPasswordReturns401NotServerError() throws Exception {
        login(EMAIL, "ə".repeat(40)) // 80 bayt > 72
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void deactivatedUserCannotLogin() throws Exception {
        owner.deactivate(clock.instant());

        login(EMAIL, PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    void userOfSuspendedTenantCannotLogin() throws Exception {
        tenant.suspend(clock.instant());

        login(EMAIL, PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TENANT_SUSPENDED"));
    }

    @Test
    void invalidRequestReturns400() throws Exception {
        login("e-poct-deyil", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void meReturnsCurrentUserForValidToken() throws Exception {
        String body = login(EMAIL, PASSWORD).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String token = JsonPath.read(body, "$.accessToken");

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(owner.getId().toString()))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.tenant.name").value("Test Şirkəti"));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password)));
    }
}
