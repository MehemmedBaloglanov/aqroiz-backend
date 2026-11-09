package az.aqroiz.identity.internal;
import az.aqroiz.shared.error.BusinessRuleException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Giriş (login) və cari istifadəçinin məlumatı.
 */
@Service
public class AuthService {

    /** Bcrypt yalnız ilk 72 baytı nəzərə alır; daha uzun şifrəni yoxlamırıq. */
    static final int MAX_PASSWORD_BYTES = 72;

    private final UserAccountRepository users;
    private final TenantRepository tenants;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final Clock clock;

    /** Mövcud olmayan e-poçt üçün yoxlama vaxtını bərabərləşdirən saxta heş. */
    private final String dummyPasswordHash;

    public AuthService(UserAccountRepository users, TenantRepository tenants, PasswordEncoder passwordEncoder,
                       TokenService tokenService, Clock clock) {
        this.users = users;
        this.tenants = tenants;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode("aqroiz-timing-equalizer");
    }

    /**
     * E-poçt və şifrəni yoxlayır, uğurlu olsa token verir və son giriş vaxtını qeyd edir.
     *
     * @throws BadCredentialsException e-poçt və ya şifrə yanlışdır (hansının səhv olduğu bildirilmir)
     * @throws DisabledException       istifadəçi hesabı bağlanıb
     * @throws LockedException         şirkətin girişi dayandırılıb
     */
    @Transactional
    public LoginResult login(String email, String password) {
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw invalidCredentials();
        }

        Optional<UserAccount> found = findByEmail(email);
        if (found.isEmpty()) {
            passwordEncoder.matches(password, dummyPasswordHash); // cavab vaxtı e-poçtun mövcudluğunu açıqlamasın
            throw invalidCredentials();
        }

        UserAccount user = found.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw invalidCredentials();
        }

        Tenant tenant = requireAccessAllowed(user);
        user.recordLogin(clock.instant()); // tranzaksiya bitəndə Hibernate özü UPDATE edəcək
        return new LoginResult(tokenService.issue(user), user, tenant);
    }

    /**
     * Tokendəki istifadəçinin hazırkı məlumatı. Token verildikdən sonra hesab bağlanıbsa, giriş rədd edilir.
     */
    @Transactional(readOnly = true)
    public Account currentAccount(UUID userId) {
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new DisabledException("İstifadəçi hesabı tapılmadı."));
        Tenant tenant = requireAccessAllowed(user);
        return new Account(user, tenant);
    }

    private Tenant requireAccessAllowed(UserAccount user) {
        if (!user.isActive()) {
            throw new DisabledException("İstifadəçi hesabı bağlanıb. Şirkət rəhbərinə müraciət edin.");
        }
        Tenant tenant = tenants.findById(user.getTenantId())
                .orElseThrow(() -> new IllegalStateException("İstifadəçinin şirkəti tapılmadı: " + user.getId()));
        if (!tenant.isActive()) {
            throw new LockedException("Şirkətin sistemə girişi dayandırılıb.");
        }
        return tenant;
    }

    private Optional<UserAccount> findByEmail(String email) {
        try {
            return users.findByEmail(UserAccount.normalizeEmail(email));
        } catch (BusinessRuleException invalidEmail) {
            return Optional.empty();
        }
    }

    private static BadCredentialsException invalidCredentials() {
        return new BadCredentialsException("E-poçt və ya şifrə yanlışdır.");
    }

    /** Uğurlu girişin nəticəsi. */
    public record LoginResult(IssuedToken token, UserAccount user, Tenant tenant) {
    }

    /** İstifadəçi və onun şirkəti. */
    public record Account(UserAccount user, Tenant tenant) {
    }
}