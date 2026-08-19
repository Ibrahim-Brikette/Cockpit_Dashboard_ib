package com.dynamicdashboard.cockpit.shared.security;
import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.identity.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

/**
 * Résout l'utilisateur courant à partir du contexte de sécurité (JWT).
 *
 * IMPORTANT (corrigé) : cette classe ne référence plus un nom codé en dur en JAVA
 * ("ahaddad" / "Amine Haddad") — ça cassait Dashboard/Query/Analytics/Audit dès
 * qu'on travaillait sur une base locale sans le seeder de démo de Moez.
 *
 * Comportement maintenant :
 *  1. Si un JWT valide est présent -> on résout le vrai utilisateur authentifié.
 *  2. Sinon, si `cockpit.security.default-username` est configuré (variable
 *     d'environnement COCKPIT_DEFAULT_USERNAME) ET que ce compte existe en base
 *     -> on l'utilise. C'est comme ça que STAGING continue d'afficher "Amine Haddad"
 *     de façon fiable (peu importe l'ordre du seeder), en le déclarant explicitement
 *     dans docker-compose.staging.yml, PAS en dur dans le code source.
 *  3. Sinon (dev local, rien de configuré) -> on prend n'importe quel compte déjà
 *     existant en base (le premier créé), pour respecter les contraintes
 *     owner_id NOT NULL sur Dashboard/Query.
 *  4. Si la base ne contient vraiment AUCUN compte utilisateur -> erreur explicite.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CurrentUserService {
    private final UserAccountRepository userAccountRepository;

    @Value("${cockpit.security.default-username:}")
    private String defaultUsername;

    @Transactional(readOnly = true)
    public UserAccountEntity getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            String name = auth.getName();
            Optional<UserAccountEntity> resolved = userAccountRepository.findByUsername(name)
                    .or(() -> userAccountRepository.findByDisplayName(name));
            if (resolved.isPresent()) {
                return resolved.get();
            }
            log.debug("Utilisateur authentifié '{}' introuvable en base (identity).", name);
        }

        if (defaultUsername != null && !defaultUsername.isBlank()) {
            Optional<UserAccountEntity> configured = userAccountRepository.findByUsername(defaultUsername);
            if (configured.isPresent()) {
                return configured.get();
            }
            log.warn("cockpit.security.default-username='{}' configuré mais introuvable en base.", defaultUsername);
        }

        return userAccountRepository.findFirstByOrderByCreatedAtAsc()
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun compte utilisateur n'existe encore dans la table identity.user_account. "
                                + "Créez au moins un compte (via l'API d'inscription ou un insert manuel minimal) "
                                + "avant de pouvoir créer des dashboards/requêtes. "
                                + "Cela n'a rien à voir avec les données KPI de démo."));
    }
}