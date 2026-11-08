package az.aqroiz.identity.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    /** E-poçt əvvəlcə UserAccount.normalizeEmail(...) ilə normallaşdırılmalıdır. */
    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    List<UserAccount> findByTenantIdOrderByFullNameAsc(UUID tenantId);
}