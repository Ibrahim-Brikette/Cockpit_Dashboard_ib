package com.dynamicdashboard.cockpit.identity.repository;
import com.dynamicdashboard.cockpit.identity.domain.UserGroupMembershipEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserGroupMembershipRepository extends JpaRepository<UserGroupMembershipEntity, UUID> {
    List<UserGroupMembershipEntity> findByUserId(UUID userId);
    List<UserGroupMembershipEntity> findByGroupId(UUID groupId);
}
