package com.dynamicdashboard.cockpit.query.repository;
import com.dynamicdashboard.cockpit.query.domain.DataQueryEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DataQueryRepository extends JpaRepository<DataQueryEntity, UUID> {
    Optional<DataQueryEntity> findByOwnerId(UUID ownerId);
    List<DataQueryEntity> findAllByTenantId(UUID tenantId);
    List<DataQueryEntity> findByOwnerIdAndTenantId(UUID ownerId, UUID tenantId);
}
