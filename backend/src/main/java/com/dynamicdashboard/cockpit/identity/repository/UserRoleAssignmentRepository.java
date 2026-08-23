package com.dynamicdashboard.cockpit.identity.repository;

import com.dynamicdashboard.cockpit.identity.domain.UserRoleAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignmentEntity, UUID> {

    List<UserRoleAssignmentEntity> findByUser_Id(UUID userId);

    Optional<UserRoleAssignmentEntity> findByUser_IdAndRole_Id(UUID userId, UUID roleId);

    @Modifying
    @Query("delete from UserRoleAssignmentEntity ura where ura.user.id = :userId and ura.role.id = :roleId")
    void deleteByUserIdAndRoleId(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

    @Modifying
    @Query("delete from UserRoleAssignmentEntity ura where ura.user.id = :userId")
    void deleteByUser_Id(@Param("userId") UUID userId);
}
