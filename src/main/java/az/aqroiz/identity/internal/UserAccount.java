package az.aqroiz.identity.internal;

import az.aqroiz.identity.Role;
import az.aqroiz.shared.error.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Şirkətin işçisi (sistem istifadəçisi). Cədvəl: users (V1).
 * <p>
 * Şifrənin özü bu sinfə heç vaxt gəlmir, yalnız onun heş-i.
 */
@Entity
@Table(name = "users")
public class UserAccount {

    private static final int EMAIL_MAX_LENGTH = 254;
    private static final int FULL_NAME_MAX_LENGTH = 120;
    private static final int PHONE_MAX_LENGTH = 20;

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "email", nullable = false, length = EMAIL_MAX_LENGTH)
    private String email;

    @Column(name = "full_name", nullable = false, length = FULL_NAME_MAX_LENGTH)
    private String fullName;

    @Column(name = "phone", length = PHONE_MAX_LENGTH)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    /** Yalnız JPA (Hibernate) üçün. */
    protected UserAccount() {
    }

    /**
     * Yeni istifadəçi yaradır.
     *
     * @param passwordHash artıq heşlənmiş şifrə (PasswordEncoder-dən)
     */
    public static UserAccount create(UUID tenantId, String email, String fullName, String phone,
                                     String passwordHash, Role role, Instant now) {
        Objects.requireNonNull(now, "now");
        UserAccount user = new UserAccount();
        user.id = UUID.randomUUID();
        user.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        user.email = normalizeEmail(email);
        user.fullName = requireValidFullName(fullName);
        user.phone = normalizePhone(phone);
        user.passwordHash = requirePasswordHash(passwordHash);
        user.role = Objects.requireNonNull(role, "role");
        user.active = true;
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }

    /**
     * E-poçtu bazadakı formata gətirir: boşluqsuz, kiçik hərflə.
     * Girişdə axtarış da bu metodla edilməlidir ki, "Ali@Mail.az" və "ali@mail.az" eyni sayılsın.
     * <p>
     * Locale.ROOT vacibdir: Azərbaycan və Türk dillərində "I".toLowerCase() → "ı" olur,
     * və "MAIL.AZ" "maıl.az"-a çevrilərdi.
     */
    public static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessRuleException("EMAIL_REQUIRED", "E-poçt boş ola bilməz.");
        }
        String normalized = email.strip().toLowerCase(Locale.ROOT);
        if (normalized.length() > EMAIL_MAX_LENGTH || normalized.indexOf('@') < 1) {
            throw new BusinessRuleException("INVALID_EMAIL", "E-poçt ünvanı düzgün deyil.");
        }
        return normalized;
    }

    public void changeRole(Role newRole, Instant now) {
        this.role = Objects.requireNonNull(newRole, "newRole");
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public void changePasswordHash(String newPasswordHash, Instant now) {
        this.passwordHash = requirePasswordHash(newPasswordHash);
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    /** İstifadəçini silmək əvəzinə girişini bağlayır. Onun qeydləri tarixçədə qalır. */
    public void deactivate(Instant now) {
        this.active = false;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public void activate(Instant now) {
        this.active = true;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    /** Uğurlu girişi qeyd edir. Profil dəyişmədiyi üçün updatedAt-a toxunmur. */
    public void recordLogin(Instant now) {
        this.lastLoginAt = Objects.requireNonNull(now, "now");
    }

    private static String requireValidFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessRuleException("FULL_NAME_REQUIRED", "Ad və soyad boş ola bilməz.");
        }
        String stripped = fullName.strip();
        if (stripped.length() > FULL_NAME_MAX_LENGTH) {
            throw new BusinessRuleException("FULL_NAME_TOO_LONG",
                    "Ad və soyad " + FULL_NAME_MAX_LENGTH + " simvoldan uzun ola bilməz.");
        }
        return stripped;
    }

    private static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String stripped = phone.strip();
        if (stripped.length() > PHONE_MAX_LENGTH) {
            throw new BusinessRuleException("PHONE_TOO_LONG",
                    "Telefon nömrəsi " + PHONE_MAX_LENGTH + " simvoldan uzun ola bilməz.");
        }
        return stripped;
    }

    private static String requirePasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash boş ola bilməz");
        }
        return passwordHash;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserAccount other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
