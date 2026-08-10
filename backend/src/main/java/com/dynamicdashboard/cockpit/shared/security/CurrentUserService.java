package com.dynamicdashboard.cockpit.shared.security;
import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.identity.repository.UserAccountRepository;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AccountStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserService implements UserDetailsService {
    private final UserAccountRepository userAccountRepository;
///    loadUserByUsername
///
///    This is the Spring Security contract method — it implements UserDetailsService, so it's called by Spring Security itself
/// , not by your application code. Specifically, it's invoked by an AuthenticationProvider
/// (typically DaoAuthenticationProvider) during the login/authentication process, when someone submits credentials.
///
///    Key traits:
///
///    Takes a username string (from the login form, HTTP Basic header, etc.)
///    Looks the user up by username or display name
///    Wraps the result in CustomUserDetails (presumably your UserDetails implementation)
/// Duplicate usernames
///
/// findByUsername is a Spring Data JPA derived query expecting at most one result.
/// If your username column doesn't have a unique constraint in the database
/// , and two rows somehow share a username
/// , Spring Data will throw IncorrectResultSizeDataAccessException at runtime instead of returning cleanly
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userAccountRepository.findByUsername(username)
                .or(() -> userAccountRepository.findByDisplayName(username))
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user with username: " + username));
    }
    @Transactional(readOnly = true)
    public UserAccountEntity getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            ///                JwtAuthenticationToken.getName() returns jwt.getSubject() — the sub claim, nothing else
            ///                the client sends the JWT it got back, and JwtAuthoritiesConverter builds a fresh JwtAuthenticationToken
            ///                from scratch each time.
            ///               That's the only thing that ever gets written to SecurityContext,
            ///                for any Bearer-token request, ever.
            UUID userId = UUID.fromString(auth.getName());
            return userAccountRepository.findById(userId)
                    .orElseThrow(() -> new NoSuchElementException(
                    "User account not found "
            ));
        }
        return getDefaultSeededUser();
    }
    @Transactional(readOnly = true)
    public UserAccountEntity getDefaultSeededUser() {
        return userAccountRepository.findByUsername("ahaddad")
                .or(() -> userAccountRepository.findByDisplayName("Amine Haddad"))
                .orElseThrow(() -> new IllegalStateException("Default user 'ahaddad' not found. Please ensure database seed V3__cockpit_seed.sql was executed."));
    }
}
