package com.dynamicdashboard.cockpit.shared.security.auth.entity;

import com.dynamicdashboard.cockpit.shared.persistence.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * One row = one login session. Created once at login, never recreated mid-session.
 *
 * WHY ROTATION WAS REMOVED:
 *   Old design burned this token on every /refresh and inserted a new row.
 *   Over an 8h session that created 32 rows (one every 15 min) with no real gain:
 *   the HttpOnly + Secure + SameSite=Strict cookie already prevents JavaScript/XSS
 *   from reading the token, which was the threat rotation tried to detect.
 *   One row per session is simpler, auditable, and equally secure given the cookie.
 *
 * TIMEOUT TRACKING (no session table needed):
 *   - lastUsedAt:      updated on every /refresh → idle-timeout check (30 min).
 *   - originalLoginAt: set once at login, never changes → absolute-timeout check (8h).
 *
 * CONCURRENT SESSION SUPPORT:
 *   - sessionId: UUID generated once at login, grouping key for revocation.
 *   - COUNT(userId WHERE revoked=false) = number of active sessions for this user.
 */
@Getter
@Setter
@Entity
@Table(
    name = "refresh_token",
    schema = "cockpit",
    indexes = {
        @Index(name = "idx_refresh_token_hash",       columnList = "token_hash", unique = true),
        @Index(name = "idx_refresh_token_session_id", columnList = "session_id"),
        @Index(name = "idx_refresh_token_user_id",    columnList = "user_id")
    }
)
public class RefreshTokenEntity extends AbstractEntity {

    /** SHA-256 hex digest (64 chars) of the raw token. The raw token never reaches the DB. */
    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    /**
     * Grouping key — generated once at login, never changes.
     * Logout and timeouts use this to revoke the session with a single UPDATE.
     * Not a FK — this table is the only session record that exists.
     */
    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Set once at login, never updated.
     * Absolute-timeout check: now > originalLoginAt + 8h → session dead (spec 5.2).
     */
    @Column(name = "original_login_at", nullable = false, updatable = false)
    private Instant originalLoginAt;

    /**
     * Updated on every successful /refresh call.
     * Idle-timeout check: now > lastUsedAt + 30min → session dead (spec 5.2).
     * Initialized to originalLoginAt at login (the login itself counts as first use).
     */
    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    /** Set to true on logout, idle-timeout, or absolute-timeout. */
    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    // -------------------------------------------------------------------------
    // OLD ROTATION DESIGN — commented, not deleted
    // -------------------------------------------------------------------------
    // "used" existed when every /refresh burned this token and issued a new row.
    // Presenting a used=true token again was the compromise/theft detection signal.
    // Removed because rotation created 32 rows per 8h session with no meaningful
    // security gain over the HttpOnly cookie protection already in place.
    //
    // @Column(name = "used", nullable = false)
    // private boolean used = false;
    // -------------------------------------------------------------------------
}
