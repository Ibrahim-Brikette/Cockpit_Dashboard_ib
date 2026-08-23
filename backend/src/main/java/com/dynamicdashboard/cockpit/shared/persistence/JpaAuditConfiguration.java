package com.dynamicdashboard.cockpit.shared.persistence;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
@Configuration
@EnableJpaAuditing(auditorAwareRef = "securityAuditorAware")
public class JpaAuditConfiguration {
    @Bean
    AuditorAware<String> securityAuditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken) {
                // Old: returned "ahaddad" — a hardcoded real username tied to a specific
                // seeded user. If that user was deleted the string became a dangling reference.
                // Replaced with "system" — a neutral label that is never tied to any real user row.
                return Optional.of("system");
            }
            return Optional.ofNullable(authentication.getName()).filter(value -> !value.isBlank())
                    .or(() -> Optional.of("system")); // old fallback was "ahaddad"
        };
    }
}
