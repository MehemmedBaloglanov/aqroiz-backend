package az.aqroiz.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import az.aqroiz.identity.Role;
import az.aqroiz.shared.error.BusinessRuleException;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserAccountTest {

    private static final Instant NOW = Instant.parse("2026-11-04T08:00:00Z");
    private static final Instant LATER = Instant.parse("2026-11-04T09:00:00Z");
    private static final UUID TENANT_ID = UUID.randomUUID();

    @Test
    void createNormalizesEmailAndStartsActive() {
        UserAccount user = UserAccount.create(TENANT_ID, "  Kenan@Mail.AZ ", " Kənan Həsənov ", " ",
                "{bcrypt}hash", Role.OWNER, NOW);

        assertThat(user.getEmail()).isEqualTo("kenan@mail.az");
        assertThat(user.getFullName()).isEqualTo("Kənan Həsənov");
        assertThat(user.getPhone()).isNull();
        assertThat(user.isActive()).isTrue();
        assertThat(user.getRole()).isEqualTo(Role.OWNER);
        assertThat(user.getLastLoginAt()).isNull();
    }

    /**
     * Kompüterin dili Türk/Azərbaycan olanda adi toLowerCase() "I" → "ı" edir.
     * normalizeEmail Locale.ROOT istifadə etdiyi üçün nəticə dəyişməməlidir.
     */
    @Test
    void emailNormalizationDoesNotDependOnSystemLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));

            assertThat(UserAccount.normalizeEmail("ALI@MAIL.AZ")).isEqualTo("ali@mail.az");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void emailWithoutAtSignIsRejected() {
        assertThatThrownBy(() -> UserAccount.normalizeEmail("kenan.mail.az"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("INVALID_EMAIL");
    }

    @Test
    void blankPasswordHashIsRejected() {
        assertThatThrownBy(() -> UserAccount.create(TENANT_ID, "a@b.az", "Ad Soyad", null, " ", Role.VIEWER, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deactivateAndRecordLogin() {
        UserAccount user = UserAccount.create(TENANT_ID, "a@b.az", "Ad Soyad", null,
                "{bcrypt}hash", Role.RECEIVER, NOW);

        user.recordLogin(LATER);
        assertThat(user.getLastLoginAt()).isEqualTo(LATER);
        assertThat(user.getUpdatedAt()).isEqualTo(NOW);

        user.deactivate(LATER);
        assertThat(user.isActive()).isFalse();
        assertThat(user.getUpdatedAt()).isEqualTo(LATER);
    }
}
