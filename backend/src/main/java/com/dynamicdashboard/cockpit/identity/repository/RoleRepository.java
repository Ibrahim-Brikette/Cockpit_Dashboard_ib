package com.dynamicdashboard.cockpit.identity.repository;
import com.dynamicdashboard.cockpit.identity.domain.RoleEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {
    Optional<RoleEntity> findByRoleName(String roleName);
    @Query("""
            select r.roleName
            from UserAccountEntity ua
            join UserRoleAssignmentEntity ura on ura.user.id = ua.id
            join RoleEntity r on r.id = ura.role.id
            where ua.id = :userId
        """)
    List<String> findPermissionsByUserId(@Param("userId")UUID userId);
}
