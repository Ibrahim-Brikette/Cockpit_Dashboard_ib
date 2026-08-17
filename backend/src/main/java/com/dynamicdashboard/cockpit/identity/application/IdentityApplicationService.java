package com.dynamicdashboard.cockpit.identity.application;

import com.dynamicdashboard.cockpit.identity.application.dto.CreateUserRequest;
import com.dynamicdashboard.cockpit.identity.application.dto.UserAccountDto;
import com.dynamicdashboard.cockpit.identity.application.mapper.IdentityMapper;
import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.identity.repository.UserAccountRepository;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AccountStatus;
import com.dynamicdashboard.cockpit.shared.security.CurrentUserService;
import com.dynamicdashboard.cockpit.shared.security.MailService;
import com.dynamicdashboard.cockpit.shared.security.auth.entity.EmailVerificationTokenEntity;
import com.dynamicdashboard.cockpit.shared.security.auth.repository.EmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityApplicationService {

    /** Verification link TTL: 24h — longer than password reset (5 min) because this is account setup, not an emergency. */
    private static final long VERIFICATION_TOKEN_TTL_HOURS = 24;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserAccountRepository            userAccountRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final IdentityMapper                   identityMapper;
    private final CurrentUserService               currentUserService;
    private final PasswordEncoder                  passwordEncoder;
    private final MailService                      mailService;

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

    // =========================================================================
    // STANDALONE-only — user creation with email verification
    // Called by IdentityController which guards this behind @ConditionalOnProperty
    // =========================================================================

    /**
     * Creates a new user account and sends an activation email.
     *
     * The admin provides username, email, displayName, and tenantId.
     * No password is set at creation time — the user chooses their own password
     * when they click the verification link.
     *
     * Steps:
     *   1. Validate uniqueness of username and email.
     *   2. Persist the user with status=PENDING and a placeholder password hash
     *      (a BCrypt hash of a random UUID — valid format, matches no known input).
     *   3. Generate a 32-byte cryptographically random token, store its SHA-256 hash.
     *   4. Send the activation email with the raw token in the link.
     *
     * The placeholder password prevents Spring Security from throwing
     * IllegalArgumentException if the account is somehow reached at login
     * before verification (it will simply fail the password check and return 401).
     *
     * @throws IllegalArgumentException if username or email is already taken
     */
    @Transactional
    public UserAccountDto createUser(CreateUserRequest request) {

        // 1. Uniqueness checks — fail fast before writing anything
        if (userAccountRepository.findByUsername(request.username()).isPresent()) {
            throw new IllegalArgumentException("Username is already taken: " + request.username());
        }
        if (userAccountRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email is already registered: " + request.email());
        }

        // 2. Create the user — PENDING status means they cannot log in yet
        UserAccountEntity user = new UserAccountEntity();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setDisplayName(request.displayName());
        user.setTenantId(request.tenantId());
        user.setAccountStatus(AccountStatus.PENDING);

        // Placeholder password: BCrypt hash of a random UUID that is immediately discarded.
        // It is a syntactically valid BCrypt hash so Spring Security won't throw on load,
        // but it matches no known input — the account cannot be used until verification.
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));

        userAccountRepository.save(user);

        // 3. Generate verification token — raw value leaves only via the email link
        String rawToken = generateRawToken();
        EmailVerificationTokenEntity token = new EmailVerificationTokenEntity();
        token.setTokenHash(sha256(rawToken));
        token.setUserId(user.getId());
        token.setExpiresAt(Instant.now().plus(VERIFICATION_TOKEN_TTL_HOURS, ChronoUnit.HOURS));
        token.setUsed(false);
        emailVerificationTokenRepository.save(token);

        // DEBUG — remove before going to production
        // Lets you test POST /api/auth/verify-email in Postman without a real mail server
        log.debug("[DEV] Verification token for userId={}: {}", user.getId(), rawToken);

        // 4. Send the activation email — failure is logged, never rolls back the transaction
        mailService.sendVerificationEmail(user.getEmail(), user.getDisplayName(), rawToken);

        log.info("User created (PENDING): userId={} username={}", user.getId(), user.getUsername());
        return identityMapper.toDto(user);
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /** 32 bytes from SecureRandom → Base64url without padding → 43-char opaque token. */
    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String raw) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
