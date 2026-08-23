package com.dynamicdashboard.cockpit.identity.repository;
import com.dynamicdashboard.cockpit.identity.domain.PermissionEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {
    Optional<PermissionEntity> findByPermissionCode(String permissionCode);
}
