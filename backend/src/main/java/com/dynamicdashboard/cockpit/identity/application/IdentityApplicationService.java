package com.dynamicdashboard.cockpit.identity.application;

import com.dynamicdashboard.cockpit.identity.application.dto.UserAccountDto;
import com.dynamicdashboard.cockpit.identity.application.mapper.IdentityMapper;
import com.dynamicdashboard.cockpit.identity.repository.UserAccountRepository;
import com.dynamicdashboard.cockpit.shared.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IdentityApplicationService {

    private final UserAccountRepository userAccountRepository;
    private final IdentityMapper        identityMapper;
    private final CurrentUserService    currentUserService;

    // -------------------------------------------------------------------------
    // OLD getCurrentUser — commented, not deleted
    // -------------------------------------------------------------------------
    // Was hardcoded to look up "ahaddad" regardless of who was authenticated.
    // If ahaddad did not exist it created a fake user row with a truncated
    // password hash — completely disconnected from the real authenticated user.
    // Removed because /me must return the profile of whoever holds the JWT,
    // not a fixed dev placeholder. Login is the only entry point — there is
    // no concept of a "default user" when unauthenticated.
    //
    // @Transactional
    // public UserAccountDto getCurrentUser() {
    //     UserAccountEntity user = userAccountRepository.findByUsername("ahaddad")
    //             .orElseGet(() -> {
    //                 UserAccountEntity newUser = new UserAccountEntity();
    //                 newUser.setUsername("ahaddad");
    //                 newUser.setEmail("amine.haddad@prestacode.com");
    //                 newUser.setDisplayName("Amine Haddad");
    //                 newUser.setPasswordHash("$2a$10$7Q9b9K...");
    //                 newUser.setAccountStatus(AccountStatus.ACTIVE);
    //                 newUser.setLastLoginAt(Instant.now());
    //                 return userAccountRepository.save(newUser);
    //             });
    //     return identityMapper.toDto(user);
    // }
    // -------------------------------------------------------------------------

    /**
     * Returns the profile of the currently authenticated user.
     * Reads sub (userId UUID) from the JWT already validated by Spring Security,
     * then fetches the full row from the DB.
     * No fallback — if there is no authenticated user the request never reaches here
     * because Spring rejects it at the filter level before any service runs.
     */
    @Transactional(readOnly = true)
    public UserAccountDto getCurrentUser() {
        return identityMapper.toDto(currentUserService.getCurrentUser());
    }

    /**
     * Returns the profile of the user identified by id.
     * Security rule (enforced in the controller via @PreAuthorize):
     *   - the authenticated user is fetching their own profile, OR
     *   - the authenticated user has ROLE_TENANT_ADMIN or ROLE_SYSTEM_ADMIN
     */
    @Transactional(readOnly = true)
    public UserAccountDto getUserById(UUID id) {
        return userAccountRepository.findById(id)
                .map(identityMapper::toDto)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<UserAccountDto> getAllUsers() {
        return userAccountRepository.findAll().stream()
                .map(identityMapper::toDto)
                .collect(Collectors.toList());
    }
}
