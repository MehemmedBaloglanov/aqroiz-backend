package az.aqroiz.identity.internal;

import az.aqroiz.shared.error.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Sistemdən istifadə edən şirkət (tenant). Cədvəl: tenants (V1).
 * <p>
 * Qaydalar:
 * - setter yoxdur, vəziyyət yalnız biznes metodları ilə dəyişir;
 * - vaxt kənardan (Clock-dan) verilir, entity özü vaxt götürmür.
 */
@Entity
@Table(name = "tenants")
public class Tenant {
    private static final int NAME_MAX_LENGTH = 200;
    private static final Pattern TAX_ID_PATTERN = Pattern.compile("\\d{10}");

    @Id
    private UUID id;

    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(name = "tax_id", length = 20)
    private String taxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TenantStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    /** Yalnız JPA (Hibernate) üçün. Kodda istifadə olunmur. */
    protected Tenant() {
    }

    /**
     * Yeni şirkəti qeydiyyata alır.
     *
     * @param name  şirkətin adı (boş ola bilməz)
     * @param taxId VÖEN: boş ola bilər, doludursa 10 rəqəm
     * @param now   cari vaxt (Clock-dan)
     */
    public static Tenant register(String name, String taxId, Instant now) {
        Objects.requireNonNull(now, "now");
        Tenant tenant = new Tenant();
        tenant.id = UUID.randomUUID();
        tenant.name = requireValidName(name);
        tenant.taxId = normalizeTaxId(taxId);
        tenant.status = TenantStatus.ACTIVE;
        tenant.createdAt = now;
        tenant.updatedAt = now;
        return tenant;
    }

    public void rename(String newName, Instant now) {
        this.name = requireValidName(newName);
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public void suspend(Instant now) {
        this.status = TenantStatus.SUSPENDED;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public void activate(Instant now) {
        this.status = TenantStatus.ACTIVE;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }

    private static String requireValidName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("TENANT_NAME_REQUIRED", "Şirkətin adı boş ola bilməz.");
        }
        String stripped = name.strip();
        if (stripped.length() > NAME_MAX_LENGTH) {
            throw new BusinessRuleException("TENANT_NAME_TOO_LONG",
                    "Şirkətin adı " + NAME_MAX_LENGTH + " simvoldan uzun ola bilməz.");
        }
        return stripped;
    }

    private static String normalizeTaxId(String taxId) {
        if (taxId == null || taxId.isBlank()) {
            return null;
        }
        String stripped = taxId.strip();
        if (!TAX_ID_PATTERN.matcher(stripped).matches()) {
            throw new BusinessRuleException("INVALID_TAX_ID", "VÖEN 10 rəqəmdən ibarət olmalıdır.");
        }
        return stripped;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getTaxId() { return taxId; }
    public TenantStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tenant other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
