package az.aqroiz.identity.internal;

import az.aqroiz.identity.Role;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * YALNIZ LOKAL: demo şirkət və istifadəçilər yaradır.
 * aqroiz.dev-data.enabled=true olanda işləyir (application-local.yaml).
 * Bazada artıq şirkət varsa, heç nə etmir.
 */
@Component
@ConditionalOnBooleanProperty("aqroiz.dev-data.enabled")
class IdentityDevDataLoader implements ApplicationRunner {

    static final String DEMO_PASSWORD = "Demo-12345";

    private static final Logger log = LoggerFactory.getLogger(IdentityDevDataLoader.class);

    private final TenantRepository tenants;
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    IdentityDevDataLoader(TenantRepository tenants, UserAccountRepository users,
                          PasswordEncoder passwordEncoder, Clock clock) {
        this.tenants = tenants;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (tenants.count() > 0) {
            log.info("DEV: bazada artıq şirkət var, demo məlumatları yaradılmır.");
            return;
        }

        Instant now = clock.instant();
        Tenant tenant = tenants.save(Tenant.register("Demo Meyvə İxrac MMC", null, now));
        String hash = passwordEncoder.encode(DEMO_PASSWORD);

        users.saveAll(List.of(
                UserAccount.create(tenant.getId(), "rehber@demo.aqroiz.az", "Kənan Həsənov", null, hash, Role.OWNER, now),
                UserAccount.create(tenant.getId(), "menecer@demo.aqroiz.az", "Aysel Məmmədova", null, hash, Role.MANAGER, now),
                UserAccount.create(tenant.getId(), "qebulcu@demo.aqroiz.az", "Murad Kərimov", null, hash, Role.RECEIVER, now),
                UserAccount.create(tenant.getId(), "muhasib@demo.aqroiz.az", "Leyla Orucova", null, hash, Role.ACCOUNTANT, now)));

        log.warn("DEV: demo şirkət və 4 istifadəçi yaradıldı (şifrə: {}). Yalnız lokal mühit üçündür!", DEMO_PASSWORD);
    }
}
