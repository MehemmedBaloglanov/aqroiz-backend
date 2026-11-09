package az.aqroiz.catalog.internal;
import az.aqroiz.shared.error.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Şirkətin aldığı məhsul (Xurma, Alma...). Cədvəl: products (V2).
 */
@Entity
@Table(name = "products")
public class Product {

    static final int NAME_MAX_LENGTH = 100;

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Product() {
    }

    public static Product create(UUID tenantId, String name, Instant now) {
        Objects.requireNonNull(now, "now");
        Product product = new Product();
        product.id = UUID.randomUUID();
        product.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        product.name = requireValidName(name);
        product.active = true;
        product.createdAt = now;
        product.updatedAt = now;
        return product;
    }

    public void rename(String newName, Instant now) {
        this.name = requireValidName(newName);
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    /** Yeni qəbzlərdə seçilmir; köhnə qəbzlər və qiymət tarixçəsi qalır. */
    public void deactivate(Instant now) {
        this.active = false;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public void activate(Instant now) {
        this.active = true;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    static String requireValidName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("PRODUCT_NAME_REQUIRED", "Məhsulun adı boş ola bilməz.");
        }
        String stripped = name.strip();
        if (stripped.length() > NAME_MAX_LENGTH) {
            throw new BusinessRuleException("PRODUCT_NAME_TOO_LONG",
                    "Məhsulun adı " + NAME_MAX_LENGTH + " simvoldan uzun ola bilməz.");
        }
        return stripped;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Product other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
