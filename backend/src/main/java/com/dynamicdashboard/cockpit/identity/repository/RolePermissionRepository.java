package com.dynamicdashboard.cockpit.identity.repository;
import com.dynamicdashboard.cockpit.identity.domain.RolePermissionEntity;
import com.dynamicdashboard.cockpit.identity.domain.RolePermissionEntity.RolePermissionId;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, RolePermissionId> {
    long countByIdRoleId(UUID roleId);
    @Query("""
        select p.permissionCode
        from RolePermissionEntity rp
        join rp.permission p
        where rp.role.roleName in :rolesNames         
    """)
    List<String> findPermissionsByRolesNames(@Param("rolesNames")List<String> rolesNames);


}
