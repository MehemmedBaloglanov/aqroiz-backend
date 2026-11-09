package az.aqroiz.catalog.internal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import az.aqroiz.shared.error.BusinessRuleException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductTest {

    private static final Instant NOW = Instant.parse("2026-11-09T08:00:00Z");

    @Test
    void createStripsNameAndStartsActive() {
        Product product = Product.create(UUID.randomUUID(), "  Xurma  ", NOW);

        assertThat(product.getName()).isEqualTo("Xurma");
        assertThat(product.isActive()).isTrue();
        assertThat(product.getVersion()).isNull();
    }

    @Test
    void blankNameIsRejected() {
        assertThatThrownBy(() -> Product.create(UUID.randomUUID(), "   ", NOW))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("PRODUCT_NAME_REQUIRED");
    }

    @Test
    void deactivateKeepsProductButMarksInactive() {
        Product product = Product.create(UUID.randomUUID(), "Alma", NOW);
        Instant later = NOW.plusSeconds(3600);

        product.deactivate(later);

        assertThat(product.isActive()).isFalse();
        assertThat(product.getUpdatedAt()).isEqualTo(later);
    }
}
