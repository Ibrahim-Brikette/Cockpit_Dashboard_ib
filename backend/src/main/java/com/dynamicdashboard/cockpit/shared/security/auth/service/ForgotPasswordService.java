package com.dynamicdashboard.cockpit.shared.security.auth.service;

import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.identity.repository.UserAccountRepository;
import com.dynamicdashboard.cockpit.shared.security.MailService;
import com.dynamicdashboard.cockpit.shared.security.auth.entity.PasswordResetTokenEntity;
import com.dynamicdashboard.cockpit.shared.security.auth.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Vertical slice: user-initiated forgot-password flow for STANDALONE mode.
 *
 * Security rules this use case enforces:
 *
 *   1. Silent on unknown email — always returns without error.
 *      The caller (AuthController) returns 204 regardless of outcome.
 *      Reason: a 404 response would let an attacker enumerate which emails
 *      are registered in the system.
 *
 *   2. No new token if a valid one already exists (email bombing prevention).
 *      If the user clicks "forgot password" twice within 5 minutes, only one
 *      email is sent and one token is valid at a time.
 *
 * The reset link in the email points to PasswordResetService via
 * POST /api/auth/password-reset — same endpoint used by the lockout flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cockpit.auth.mode", havingValue = "STANDALONE", matchIfMissing = true)
public class ForgotPasswordService {

    static final long RESET_TOKEN_TTL_SECONDS = 300; // 5 min

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserAccountRepository        userAccountRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailService                  mailService;

    @Transactional
    public void execute(String email) {
        UserAccountEntity user = userAccountRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // Silent return — do not reveal that this email is not registered
            log.debug("Forgot-password request for unregistered email — silent return");
            return;
        }

        // Prevent email bombing: if a valid (not used, not expired) token already exists, do nothing.
        // The user already has a live link in their inbox.
        boolean activeTokenExists = passwordResetTokenRepository
                .existsByUserIdAndUsedFalseAndExpiresAtAfter(user.getId(), Instant.now());
        if (activeTokenExists) {
            log.debug("Active reset token already exists for userId={} — skipping new token", user.getId());
            return;
        }

        String rawToken = generateRawToken();
        PasswordResetTokenEntity token = new PasswordResetTokenEntity();
        token.setTokenHash(sha256(rawToken));
        token.setUserId(user.getId());
        token.setExpiresAt(Instant.now().plusSeconds(RESET_TOKEN_TTL_SECONDS));
        token.setUsed(false);
        passwordResetTokenRepository.save(token);

        mailService.sendForgotPasswordEmail(user.getEmail(), user.getDisplayName(), rawToken);
        log.info("Forgot-password email dispatched: userId={}", user.getId());
    }

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
