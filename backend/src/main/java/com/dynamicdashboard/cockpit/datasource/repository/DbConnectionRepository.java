package com.dynamicdashboard.cockpit.datasource.repository;
import com.dynamicdashboard.cockpit.datasource.domain.DbConnectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface DbConnectionRepository extends JpaRepository<DbConnectionEntity, UUID> {
    Optional<DbConnectionEntity> findByConnectionName(String connectionName);
}
