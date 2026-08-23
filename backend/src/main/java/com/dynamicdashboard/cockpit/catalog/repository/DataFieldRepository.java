package com.dynamicdashboard.cockpit.catalog.repository;
import com.dynamicdashboard.cockpit.catalog.domain.DataFieldEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DataFieldRepository extends JpaRepository<DataFieldEntity, UUID> {
    List<DataFieldEntity> findByDataSourceId(UUID dataSourceId);
    Optional<DataFieldEntity> findByDataSourceIdAndFieldKey(UUID dataSourceId, String fieldKey);
    Optional<DataFieldEntity> findFirstByFieldKey(String fieldKey);
    Optional<DataFieldEntity> findFirstByFieldLabel(String fieldLabel);
    default Optional<DataFieldEntity> findByFieldKey(String fieldKey) {
        return findFirstByFieldKey(fieldKey);
    }
}
