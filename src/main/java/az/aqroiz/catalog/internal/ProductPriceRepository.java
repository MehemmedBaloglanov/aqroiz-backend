package az.aqroiz.catalog.internal;
import az.aqroiz.catalog.Grade;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductPriceRepository extends JpaRepository<ProductPrice, UUID> {

    /**
     * Verilən anda qüvvədə olan qiymət: effectiveFrom <= at olanlardan ən sonuncusu.
     * Eyni effectiveFrom ilə bir neçə qiymət varsa (düzəliş), sonra yaradılan qalib gəlir.
     */
    default Optional<ProductPrice> findEffective(UUID tenantId, UUID productId, Grade grade, Instant at) {
        return findFirstByTenantIdAndProductIdAndGradeAndEffectiveFromLessThanEqualOrderByEffectiveFromDescCreatedAtDesc(
                tenantId, productId, grade, at);
    }

    Optional<ProductPrice> findFirstByTenantIdAndProductIdAndGradeAndEffectiveFromLessThanEqualOrderByEffectiveFromDescCreatedAtDesc(
            UUID tenantId, UUID productId, Grade grade, Instant at);
}
