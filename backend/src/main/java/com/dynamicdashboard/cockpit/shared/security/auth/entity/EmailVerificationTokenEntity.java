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
 * One row = one account-activation link sent to a newly created user.
 *
 * Lifecycle:
 *   - Created by IdentityApplicationService.createUser() when the admin registers a new user.
 *   - Consumed by VerifyEmailService.execute() when the user clicks the link and sets a password.
 *   - After consumption: used = true, the link is permanently dead.
 *
 * The raw token is never stored here.
 * Only its SHA-256 hex digest (64 chars) lives in token_hash.
 * The raw value travels exactly once: inside the email link sent to the user.
 *
 * TTL is 24 hours — longer than password reset (5 min) because this is
 * account setup, not an emergency. The user may click the link the next morning.
 */
@Getter
@Setter
@Entity
@Table(name = "email_verification_token", schema = "cockpit")
public class EmailVerificationTokenEntity extends AbstractEntity {

    /** SHA-256 hex digest of the raw token. Raw token never persisted. */
    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    /** The user this token is intended for. Status is PENDING until verification succeeds. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** now() + 24h at creation time. VerifyEmailService rejects anything past this instant. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Flipped to true after the user successfully sets their password. Single-use guarantee. */
    @Column(name = "used", nullable = false)
    private boolean used;
}
