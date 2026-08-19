package com.dynamicdashboard.cockpit.identity.repository;
import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserAccountRepository extends JpaRepository<UserAccountEntity, UUID> {
    Optional<UserAccountEntity> findByUsername(String username);
    Optional<UserAccountEntity> findByDisplayName(String displayName);
    Optional<UserAccountEntity> findByEmail(String email);

    /** Utilisé uniquement comme filet de sécurité en environnement de dev/local
     *  quand aucun utilisateur n'est authentifié : prend n'importe quel compte
     *  existant plutôt qu'un nom codé en dur ("ahaddad"). */
    Optional<UserAccountEntity> findFirstByOrderByCreatedAtAsc();
}