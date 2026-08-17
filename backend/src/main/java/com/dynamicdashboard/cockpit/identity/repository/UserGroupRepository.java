package com.dynamicdashboard.cockpit.identity.repository;
import com.dynamicdashboard.cockpit.identity.domain.UserGroupEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserGroupRepository extends JpaRepository<UserGroupEntity, UUID> {
    Optional<UserGroupEntity> findByGroupCode(String groupCode);
}
