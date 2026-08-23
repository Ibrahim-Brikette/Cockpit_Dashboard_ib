package com.dynamicdashboard.cockpit.shared.security.auth.entity;

import com.dynamicdashboard.cockpit.shared.persistence.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * One row = one reset link delivered to the user's email.
 *
 * Used by two flows:
 *   - Lockout reset  : created by BruteForceService when the account is locked
 *   - Forgot password: created by ForgotPasswordService on user request
 *
 * The raw token is never stored here — only its SHA-256 hex digest (64 chars).
 * The raw value appears exactly once: inside the email link sent to the user.
 * PasswordResetService validates by hashing the incoming token and looking up this table.
 */
@Getter
@Setter
@Entity
@Table(name = "password_reset_token", schema = "cockpit")
public class PasswordResetTokenEntity extends AbstractEntity {

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** now() + 5 min at creation time. PasswordResetService rejects anything past this. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Flipped to true after first successful consumption — single-use guarantee. */
    @Column(name = "used", nullable = false)
    private boolean used;
}
