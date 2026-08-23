package com.dynamicdashboard.cockpit.shared.security.auth.service;

import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.identity.repository.UserAccountRepository;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AccountStatus;
import com.dynamicdashboard.cockpit.shared.security.auth.entity.EmailVerificationTokenEntity;
import com.dynamicdashboard.cockpit.shared.security.auth.repository.EmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Handles the email verification step of the account-creation flow (STANDALONE mode only).
 *
 * This service is called when a newly created user clicks the activation link in their email
 * and submits their chosen password via POST /api/auth/verify-email.
 *
 * What it does:
 *   1. Hashes the raw token from the request and looks it up in the DB.
 *   2. Validates the token is not already used and not expired.
 *   3. Validates password strength.
 *   4. Sets the user's password (BCrypt-hashed) and activates the account (PENDING → ACTIVE).
 *   5. Marks the token as used so the link cannot be reused.
 *
 * After this call the user can log in normally via POST /api/auth/login.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cockpit.auth.mode", havingValue = "STANDALONE", matchIfMissing = true)
public class VerifyEmailService {

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final UserAccountRepository            userAccountRepository;
    private final PasswordEncoder                  passwordEncoder;

    /**
     * Validates the token and activates the account with the chosen password.
     * Throws IllegalArgumentException on any validation failure — caller returns 400.
     *
     * @param rawToken  the plain token value extracted from the email link by the frontend
     * @param password  the password the user chose in the setup form
     */
    @Transactional
    public void execute(String rawToken, String password) {

        // 1. Look up the token by its SHA-256 hash — raw value never touches the DB
        EmailVerificationTokenEntity token = emailVerificationTokenRepository
                .findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        // 2. Single-use check — a used token means the account is already active
        if (token.isUsed()) {
            throw new IllegalArgumentException("Verification token has already been used. Your account is already active.");
        }

        // 3. Expiry check — the admin must resend an invitation if the link expired
        if (Instant.now().isAfter(token.getExpiresAt())) {
            throw new IllegalArgumentException("Verification token has expired. Please contact your administrator to resend the invitation.");
        }

        // 4. Password strength — same rules as password reset
        validatePasswordStrength(password);

        // 5. Load the user — must exist since the token references their userId
        UserAccountEntity user = userAccountRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 6. Set the real password (BCrypt-hashed) and activate the account
        //    The placeholder hash stored at creation time is now replaced by the user's own choice.
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setAccountStatus(AccountStatus.ACTIVE);
        userAccountRepository.save(user);

        // 7. Mark token as used — the activation link is permanently dead after this point
        token.setUsed(true);
        emailVerificationTokenRepository.save(token);

        log.info("Account activated: userId={}", user.getId());
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * Minimum password strength rules:
     *   - At least 8 characters
     *   - At least one uppercase letter
     *   - At least one lowercase letter
     *   - At least one digit
     */
    private void validatePasswordStrength(String p) {
        if (p == null || p.length() < 8)
            throw new IllegalArgumentException("Password must be at least 8 characters");
        if (p.chars().noneMatch(Character::isUpperCase))
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        if (p.chars().noneMatch(Character::isLowerCase))
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        if (p.chars().noneMatch(Character::isDigit))
            throw new IllegalArgumentException("Password must contain at least one digit");
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
