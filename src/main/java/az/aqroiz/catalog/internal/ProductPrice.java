package az.aqroiz.catalog.internal;
import az.aqroiz.catalog.Grade;
import az.aqroiz.shared.error.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.springframework.data.domain.Persistable;

/**
 * Məhsulun sort üzrə kiloqram qiyməti. Cədvəl: product_prices (V2).
 * <p>
 * DƏYİŞMƏZDİR: yaradıldıqdan sonra heç vaxt dəyişdirilmir və silinmir.
 * Yeni qiymət = yeni sətir. Bu, tarixçəni və auditi qoruyur.
 */
@Entity
@Table(name = "product_prices")
@Immutable
public class ProductPrice implements Persistable<UUID> {

    /** Saat fərqinə görə “indi” göndərilən qiymət rədd edilməsin. */
    static final Duration PAST_TOLERANCE = Duration.ofMinutes(1);

    static final int SCALE = 4;
    private static final BigDecimal MAX_PRICE = new BigDecimal("999999.9999"); // numeric(10, 4)

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false, length = 5, updatable = false)
    private Grade grade;

    @Column(name = "price_per_kg", nullable = false, precision = 10, scale = SCALE, updatable = false)
    private BigDecimal pricePerKg;

    @Column(name = "effective_from", nullable = false, updatable = false)
    private Instant effectiveFrom;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    /** Spring Data-ya obyektin yeni olduğunu bildirir (aşağıdakı izaha baxın). Bazada saxlanmır. */
    @Transient
    private boolean isNew = true;

    protected ProductPrice() {
    }

    /**
     * Yeni qiymət yaradır.
     *
     * @param effectiveFrom qüvvəyə minmə anı: indi və ya gələcək (1 dəqiqə tolerantlıqla)
     * @param createdBy     qiyməti daxil edən istifadəçi
     * @param now           cari vaxt (Clock-dan)
     */
    public static ProductPrice create(UUID tenantId, UUID productId, Grade grade, BigDecimal pricePerKg,
                                      Instant effectiveFrom, UUID createdBy, Instant now) {
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(effectiveFrom, "effectiveFrom");
        if (effectiveFrom.isBefore(now.minus(PAST_TOLERANCE))) {
            throw new BusinessRuleException("PRICE_IN_PAST",
                    "Keçmiş vaxt üçün qiymət təyin etmək olmaz. Qiymət indidən və ya gələcəkdən qüvvəyə minə bilər.");
        }

        ProductPrice price = new ProductPrice();
        price.id = UUID.randomUUID();
        price.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        price.productId = Objects.requireNonNull(productId, "productId");
        price.grade = Objects.requireNonNull(grade, "grade");
        price.pricePerKg = normalizePrice(pricePerKg);
        price.effectiveFrom = effectiveFrom;
        price.createdBy = Objects.requireNonNull(createdBy, "createdBy");
        price.createdAt = now;
        return price;
    }

    static BigDecimal normalizePrice(BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new BusinessRuleException("PRICE_NOT_POSITIVE", "Qiymət sıfırdan böyük olmalıdır.");
        }
        if (price.stripTrailingZeros().scale() > SCALE) {
            throw new BusinessRuleException("PRICE_TOO_PRECISE",
                    "Qiymət ən çox " + SCALE + " onluq rəqəmlə yazıla bilər.");
        }
        if (price.compareTo(MAX_PRICE) > 0) {
            throw new BusinessRuleException("PRICE_TOO_LARGE", "Qiymət çox böyükdür.");
        }
        return price.setScale(SCALE); // yuvarlaqlaşdırma yoxdur: yuxarıda 4 onluqdan çox olmadığı yoxlanılıb
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getProductId() { return productId; }
    public Grade getGrade() { return grade; }
    public BigDecimal getPricePerKg() { return pricePerKg; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductPrice other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
