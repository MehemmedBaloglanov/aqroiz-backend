package az.aqroiz.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import az.aqroiz.TestcontainersConfiguration;
import az.aqroiz.identity.Role;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entity-lərin V1 cədvəllərinə uyğunluğunu və bazadakı məhdudiyyətləri yoxlayır.
 * Real PostgreSQL (Testcontainers). Hər test tranzaksiyada işləyir və sonda geri qaytarılır.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
public class IdentityRepositoryTests {

    private static final Instant NOW = Instant.parse("2026-11-04T08:00:00Z");

    @Autowired
    private TenantRepository tenants;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private EntityManager entityManager;

    @Test
    void tenantAndUserAreStoredAndLoaded() {
        Tenant tenant = tenants.saveAndFlush(Tenant.register("Göyçay Meyvə İxrac MMC", "1234567890", NOW));
        users.saveAndFlush(UserAccount.create(tenant.getId(), "Kenan@Mail.AZ", "Kənan Həsənov",
                "050 555 00 00", "{bcrypt}hash", Role.OWNER, NOW));

        entityManager.clear(); // yaddaşdakı obyektləri unuduruq ki, aşağıda bazadan yenidən oxunsun

        UserAccount loaded = users.findByEmail("kenan@mail.az").orElseThrow();
        assertThat(loaded.getTenantId()).isEqualTo(tenant.getId());
        assertThat(loaded.getFullName()).isEqualTo("Kənan Həsənov");
        assertThat(loaded.getRole()).isEqualTo(Role.OWNER);
        assertThat(loaded.isActive()).isTrue();
        assertThat(loaded.getCreatedAt()).isEqualTo(NOW);
        assertThat(loaded.getVersion()).isZero();

        Tenant loadedTenant = tenants.findById(tenant.getId()).orElseThrow();
        assertThat(loadedTenant.getName()).isEqualTo("Göyçay Meyvə İxrac MMC");
        assertThat(loadedTenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    void emailMustBeUniqueAcrossAllTenants() {
        Tenant first = tenants.saveAndFlush(Tenant.register("Birinci", null, NOW));
        Tenant second = tenants.saveAndFlush(Tenant.register("İkinci", null, NOW));
        users.saveAndFlush(UserAccount.create(first.getId(), "ali@mail.az", "Əli", null, "{bcrypt}h", Role.OWNER, NOW));

        assertThatThrownBy(() -> users.saveAndFlush(
                UserAccount.create(second.getId(), "ALI@mail.az", "Əli 2", null, "{bcrypt}h", Role.OWNER, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void userRequiresExistingTenant() {
        UUID missingTenantId = UUID.randomUUID();

        assertThatThrownBy(() -> users.saveAndFlush(
                UserAccount.create(missingTenantId, "x@mail.az", "X", null, "{bcrypt}h", Role.VIEWER, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void taxIdMustBeUnique() {
        tenants.saveAndFlush(Tenant.register("Birinci", "1234567890", NOW));

        assertThatThrownBy(() -> tenants.saveAndFlush(Tenant.register("İkinci", "1234567890", NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
