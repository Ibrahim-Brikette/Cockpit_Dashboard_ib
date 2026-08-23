package com.dynamicdashboard.cockpit.shared.security;
import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.identity.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrentUserService implements UserDetailsService {
    private final UserAccountRepository userAccountRepository;

    @Value("${cockpit.security.default-username:}")
    private String defaultUsername;

    /**
     * Spring Security contract — called by DaoAuthenticationProvider during STANDALONE login.
     * Looks up by username, falls back to displayName for flexibility.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userAccountRepository.findByUsername(username)
                .or(() -> userAccountRepository.findByDisplayName(username))
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user with username: " + username));
    }

    /**
     * Resolves the currently authenticated user from the JWT security context.
     *
     * Resolution order:
     *  1. Valid JWT present → resolve by UUID (sub claim).
     *  2. cockpit.security.default-username configured → use that account (staging fallback).
     *  3. Fallback to first account in DB (local dev without JWT).
     *  4. No accounts exist → explicit error.
     */
    @Transactional(readOnly = true)
    public UserAccountEntity getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            String name = auth.getName();
            // JWT sub claim is the user UUID in STANDALONE mode
            try {
                UUID userId = UUID.fromString(name);
                Optional<UserAccountEntity> byId = userAccountRepository.findById(userId);
                if (byId.isPresent()) return byId.get();
            } catch (IllegalArgumentException ignored) {
                // Not a UUID — fall through to username/displayName lookup
            }
            Optional<UserAccountEntity> resolved = userAccountRepository.findByUsername(name)
                    .or(() -> userAccountRepository.findByDisplayName(name));
            if (resolved.isPresent()) return resolved.get();
            log.debug("Authenticated user '{}' not found in identity store.", name);
        }

        if (defaultUsername != null && !defaultUsername.isBlank()) {
            Optional<UserAccountEntity> configured = userAccountRepository.findByUsername(defaultUsername);
            if (configured.isPresent()) return configured.get();
            log.warn("cockpit.security.default-username='{}' configured but not found in DB.", defaultUsername);
        }

        return userAccountRepository.findFirstByOrderByCreatedAtAsc()
                .orElseThrow(() -> new IllegalStateException(
                        "No user account exists in identity.user_account. "
                                + "Create at least one account before creating dashboards/queries."));
    }
}
