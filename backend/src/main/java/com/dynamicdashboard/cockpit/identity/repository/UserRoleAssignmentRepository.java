package com.dynamicdashboard.cockpit.identity.repository;
import com.dynamicdashboard.cockpit.identity.domain.UserRoleAssignmentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignmentEntity, UUID> {
    List<UserRoleAssignmentEntity> findByUserId(UUID userId);
}
