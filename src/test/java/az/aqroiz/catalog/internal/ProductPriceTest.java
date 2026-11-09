package az.aqroiz.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import az.aqroiz.catalog.Grade;
import az.aqroiz.shared.error.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductPriceTest {

    private static final Instant NOW = Instant.parse("2026-11-09T08:00:00Z");
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID PRODUCT = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();

    @Test
    void priceIsStoredWithFourDecimals() {
        ProductPrice price = price("0.95", NOW);

        assertThat(price.getPricePerKg()).isEqualTo(new BigDecimal("0.9500"));
        assertThat(price.isNew()).isTrue();
    }

    @Test
    void zeroAndNegativePricesAreRejected() {
        assertThatThrownBy(() -> price("0", NOW)).extracting("code").isEqualTo("PRICE_NOT_POSITIVE");
        assertThatThrownBy(() -> price("-1.20", NOW)).extracting("code").isEqualTo("PRICE_NOT_POSITIVE");
    }

    @Test
    void moreThanFourDecimalsIsRejectedNotRounded() {
        assertThatThrownBy(() -> price("0.95501", NOW))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("PRICE_TOO_PRECISE");
    }

    @Test
    void trailingZerosBeyondFourDecimalsAreAccepted() {
        assertThat(price("0.950000", NOW).getPricePerKg()).isEqualTo(new BigDecimal("0.9500"));
    }

    @Test
    void priceInThePastIsRejected() {
        assertThatThrownBy(() -> price("0.95", NOW.minus(Duration.ofHours(1))))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("PRICE_IN_PAST");
    }

    @Test
    void priceSecondsInThePastIsAcceptedBecauseOfClockTolerance() {
        assertThat(price("0.95", NOW.minusSeconds(20)).getEffectiveFrom()).isEqualTo(NOW.minusSeconds(20));
    }

    private static ProductPrice price(String value, Instant effectiveFrom) {
        return ProductPrice.create(TENANT, PRODUCT, Grade.I, new BigDecimal(value), effectiveFrom, USER, NOW);
    }
}
