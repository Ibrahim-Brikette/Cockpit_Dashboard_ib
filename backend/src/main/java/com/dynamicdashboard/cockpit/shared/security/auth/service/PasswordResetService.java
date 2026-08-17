package com.dynamicdashboard.cockpit.shared.security.auth.service;

import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.identity.repository.UserAccountRepository;
import com.dynamicdashboard.cockpit.shared.security.auth.entity.PasswordResetTokenEntity;
import com.dynamicdashboard.cockpit.shared.security.auth.repository.PasswordResetTokenRepository;
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
 * Vertical slice: password reset consumption for STANDALONE mode.
 *
 * Shared by two flows — both store their token in password_reset_token:
 *   - Lockout reset  : token created by BruteForceService when account is locked
 *   - Forgot password: token created by ForgotPasswordService on user request
 *
 * This use case does not care which flow created the token —
 * validation logic is identical for both.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cockpit.auth.mode", havingValue = "STANDALONE", matchIfMissing = true)
public class PasswordResetService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserAccountRepository        userAccountRepository;
    private final PasswordEncoder              passwordEncoder;

    /**
     * Validates the raw token from the email link and sets the new password.
     * On success:
     *   - password_hash updated with BCrypt(newPassword)
     *   - all lockout fields cleared (account fully unlocked)
     *   - token marked as used (single-use guarantee)
     *
     * Throws IllegalArgumentException on any validation failure.
     * AuthController catches this and returns 400.
     */
    @Transactional
    public void execute(String rawToken, String newPassword) {
        PasswordResetTokenEntity token = passwordResetTokenRepository
                .findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

        if (token.isUsed())
            throw new IllegalArgumentException("Reset token has already been used");

        if (Instant.now().isAfter(token.getExpiresAt()))
            throw new IllegalArgumentException("Reset token has expired");

        validatePasswordStrength(newPassword);

        UserAccountEntity user = userAccountRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));

        // Full lockout reset — account is completely clean after a successful reset.
        // lockout_count is also reset so the escalation ladder starts over.
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLockoutCount(0);
        user.setPermanentlyLocked(false);
        userAccountRepository.save(user);

        token.setUsed(true); // link is now dead — cannot be reused
        passwordResetTokenRepository.save(token);

        log.info("Password reset successful: userId={}", user.getId());
    }

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
