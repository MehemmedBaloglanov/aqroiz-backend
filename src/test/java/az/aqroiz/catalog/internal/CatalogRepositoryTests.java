package az.aqroiz.catalog.internal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import az.aqroiz.TestcontainersConfiguration;
import az.aqroiz.catalog.Grade;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * “Qüvvədə olan qiymət” sorğusu və bazadakı məhdudiyyətlər. Real PostgreSQL.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class CatalogRepositoryTests {

    private static final Instant T0 = Instant.parse("2026-11-09T06:00:00Z");
    private static final Instant T1 = T0.plus(Duration.ofHours(4));
    private static final Instant T2 = T0.plus(Duration.ofDays(1));

    @Autowired
    private ProductRepository products;

    @Autowired
    private ProductPriceRepository prices;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID tenantA;
    private UUID tenantB;
    private UUID userA;
    private Product xurma;

    @BeforeEach
    void setUp() {
        tenantA = insertTenant("A Şirkəti");
        tenantB = insertTenant("B Şirkəti");
        userA = insertUser(tenantA);
        xurma = products.saveAndFlush(Product.create(tenantA, "Xurma", T0));
    }

    @Test
    void effectivePriceIsTheLatestOneNotLaterThanTheGivenMoment() {
        prices.saveAndFlush(price(tenantA, xurma.getId(), Grade.I, "0.90", T0, T0));
        prices.saveAndFlush(price(tenantA, xurma.getId(), Grade.I, "0.95", T1, T0));
        prices.saveAndFlush(price(tenantA, xurma.getId(), Grade.I, "1.00", T2, T0)); // sabahın qiyməti

        assertThat(prices.findEffective(tenantA, xurma.getId(), Grade.I, T0.minusSeconds(1))).isEmpty();
        assertThat(effectivePrice(T0.plusSeconds(60))).isEqualByComparingTo("0.90");
        assertThat(effectivePrice(T1)).isEqualByComparingTo("0.95");
        assertThat(effectivePrice(T1.plus(Duration.ofHours(2)))).isEqualByComparingTo("0.95");
        assertThat(effectivePrice(T2.plusSeconds(1))).isEqualByComparingTo("1.00");
    }

    @Test
    void correctionWithTheSameEffectiveFromWins() {
        prices.saveAndFlush(price(tenantA, xurma.getId(), Grade.I, "9.50", T1, T0));             // səhv
        prices.saveAndFlush(price(tenantA, xurma.getId(), Grade.I, "0.95", T1, T0.plusSeconds(30))); // düzəliş

        assertThat(effectivePrice(T1)).isEqualByComparingTo("0.95");
    }

    @Test
    void gradesHaveSeparatePrices() {
        prices.saveAndFlush(price(tenantA, xurma.getId(), Grade.II, "0.70", T0, T0));

        assertThat(prices.findEffective(tenantA, xurma.getId(), Grade.I, T1)).isEmpty();
        assertThat(prices.findEffective(tenantA, xurma.getId(), Grade.II, T1)).isPresent();
    }

    @Test
    void databaseRejectsPriceWhoseTenantDiffersFromProductTenant() {
        assertThatThrownBy(() -> prices.saveAndFlush(price(tenantB, xurma.getId(), Grade.I, "0.95", T0, T0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void productNameIsUniquePerTenantIgnoringCase() {
        products.saveAndFlush(Product.create(tenantB, "xurma", T0)); // başqa şirkət — icazəlidir

        assertThatThrownBy(() -> products.saveAndFlush(Product.create(tenantA, "XURMA", T0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private BigDecimal effectivePrice(Instant at) {
        return prices.findEffective(tenantA, xurma.getId(), Grade.I, at).orElseThrow().getPricePerKg();
    }

    private ProductPrice price(UUID tenantId, UUID productId, Grade grade, String value,
                               Instant effectiveFrom, Instant createdAt) {
        return ProductPrice.create(tenantId, productId, grade, new BigDecimal(value), effectiveFrom, userA, createdAt);
    }

    private UUID insertTenant(String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into tenants (id, name, status, created_at, updated_at, version)
                values (?, ?, 'ACTIVE', now(), now(), 0)
                """, id, name);
        return id;
    }

    private UUID insertUser(UUID tenantId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into users (id, tenant_id, email, full_name, password_hash, role, active, created_at, updated_at, version)
                values (?, ?, ?, 'Test', '{noop}x', 'OWNER', true, now(), now(), 0)
                """, id, tenantId, "u-" + id + "@test.az");
        return id;
    }
}
