package com.dynamicdashboard.cockpit.identity.repository;

import com.dynamicdashboard.cockpit.identity.domain.UserGroupMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserGroupMembershipRepository extends JpaRepository<UserGroupMembershipEntity, UUID> {

    List<UserGroupMembershipEntity> findByUser_Id(UUID userId);

    List<UserGroupMembershipEntity> findByGroup_Id(UUID groupId);

    Optional<UserGroupMembershipEntity> findByGroup_IdAndUser_Id(UUID groupId, UUID userId);

    @Modifying
    @Query("delete from UserGroupMembershipEntity m where m.group.id = :groupId and m.user.id = :userId")
    void deleteByGroupIdAndUserId(@Param("groupId") UUID groupId, @Param("userId") UUID userId);

    @Modifying
    @Query("delete from UserGroupMembershipEntity m where m.user.id = :userId")
    void deleteByUser_Id(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("delete from UserGroupMembershipEntity m where m.group.id = :groupId")
    void deleteByGroup_Id(@Param("groupId") UUID groupId);
}
