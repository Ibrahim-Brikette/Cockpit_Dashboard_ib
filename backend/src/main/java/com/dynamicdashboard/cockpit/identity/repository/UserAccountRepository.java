package com.dynamicdashboard.cockpit.identity.repository;
import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserAccountRepository extends JpaRepository<UserAccountEntity, UUID> {
    Optional<UserAccountEntity> findByUsername(String username);
    Optional<UserAccountEntity> findByDisplayName(String displayName);
    Optional<UserAccountEntity> findByEmail(String email);

    /** Used as a safety net in dev/local when no user is authenticated:
     *  returns any existing account rather than a hardcoded username. */
    Optional<UserAccountEntity> findFirstByOrderByCreatedAtAsc();
}
