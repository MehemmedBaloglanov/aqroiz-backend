package az.aqroiz.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import az.aqroiz.shared.error.BusinessRuleException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TenantTest {

    private static final Instant NOW = Instant.parse("2026-11-04T08:00:00Z");
    private static final Instant LATER = Instant.parse("2026-11-04T09:00:00Z");

    @Test
    void registerCreatesActiveTenantWithStrippedName() {
        Tenant tenant = Tenant.register("  Göyçay Meyvə İxrac MMC  ", "1234567890", NOW);

        assertThat(tenant.getId()).isNotNull();
        assertThat(tenant.getName()).isEqualTo("Göyçay Meyvə İxrac MMC");
        assertThat(tenant.getTaxId()).isEqualTo("1234567890");
        assertThat(tenant.isActive()).isTrue();
        assertThat(tenant.getCreatedAt()).isEqualTo(NOW);
        assertThat(tenant.getUpdatedAt()).isEqualTo(NOW);
        assertThat(tenant.getVersion()).isNull();
    }

    @Test
    void blankTaxIdIsStoredAsNull() {
        Tenant tenant = Tenant.register("Şirkət", "   ", NOW);

        assertThat(tenant.getTaxId()).isNull();
    }

    @Test
    void invalidTaxIdIsRejected() {
        assertThatThrownBy(() -> Tenant.register("Şirkət", "12345", NOW))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("INVALID_TAX_ID");
    }

    @Test
    void blankNameIsRejected() {
        assertThatThrownBy(() -> Tenant.register("  ", null, NOW))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("TENANT_NAME_REQUIRED");
    }

    @Test
    void suspendAndActivateChangeStatusAndUpdatedAt() {
        Tenant tenant = Tenant.register("Şirkət", null, NOW);

        tenant.suspend(LATER);
        assertThat(tenant.isActive()).isFalse();
        assertThat(tenant.getUpdatedAt()).isEqualTo(LATER);

        tenant.activate(LATER);
        assertThat(tenant.isActive()).isTrue();
        assertThat(tenant.getCreatedAt()).isEqualTo(NOW);
    }
}
