package az.aqroiz.identity.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    boolean existsByTaxId(String taxId);
}
