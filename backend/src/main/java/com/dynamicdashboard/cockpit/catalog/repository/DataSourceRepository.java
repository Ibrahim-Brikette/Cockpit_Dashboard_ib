package com.dynamicdashboard.cockpit.catalog.repository;
import com.dynamicdashboard.cockpit.catalog.domain.DataSourceEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DataSourceRepository extends JpaRepository<DataSourceEntity, UUID> {
    Optional<DataSourceEntity> findBySourceKey(String sourceKey);
    Optional<DataSourceEntity> findFirstBySourceLabel(String sourceLabel);
    long countByDbConnectionId(UUID dbConnectionId);
    java.util.List<DataSourceEntity> findByDbConnectionId(UUID dbConnectionId);
}
