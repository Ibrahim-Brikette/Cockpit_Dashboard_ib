package com.dynamicdashboard.cockpit.shared.security.auth.repository;

import com.dynamicdashboard.cockpit.shared.security.auth.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    /** Primary lookup: sha256(presentedRawToken) compared hash-to-hash. */
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    /**
     * Counts active (non-revoked) sessions for a user.
     * One row per session → count = number of concurrent sessions.
     * Used to enforce the max-3-sessions limit on login (spec 5.2).
     */
    long countByUserIdAndRevokedFalse(UUID userId);

    /**
     * Returns active sessions ordered by login time ascending (oldest first).
     * Used to evict the oldest session when the concurrent-session limit is exceeded.
     */
    List<RefreshTokenEntity> findByUserIdAndRevokedFalseOrderByOriginalLoginAtAsc(UUID userId);

    /**
     * Deletes the session row entirely.
     * Called on: logout (user chose to leave — no audit row needed),
     *            session eviction (oldest session kicked on new login).
     * "Not found" on the next /refresh attempt is handled by orElseThrow → clean rejection.
     */
    @Modifying
    @Query("DELETE FROM RefreshTokenEntity t WHERE t.sessionId = :sessionId")
    int deleteBySessionId(@Param("sessionId") UUID sessionId);

    /**
     * Marks session as revoked but keeps the row for audit.
     * Called on: idle-timeout, absolute-timeout — session died mid-use, not by user choice.
     *
     * OLD: this same method was also called on logout and session eviction.
     * Replaced for those two cases by deleteBySessionId above — keeping a dead row
     * after an explicit logout is unnecessary and accumulates garbage over time.
     */
    @Modifying
    @Query("UPDATE RefreshTokenEntity t SET t.revoked = true " +
           "WHERE t.sessionId = :sessionId AND t.revoked = false")
    int revokeAllBySessionId(@Param("sessionId") UUID sessionId);

    // -------------------------------------------------------------------------
    // OLD ROTATION DESIGN — commented, not deleted
    // -------------------------------------------------------------------------
    // These queries filtered on "used=false" because under rotation every burned
    // token stayed in the table marked used=true. Without rotation every active
    // token is simply revoked=false — the used field no longer exists.
    //
    // long countByUserIdAndUsedFalseAndRevokedFalse(UUID userId);
    //
    // List<RefreshTokenEntity>
    //     findByUserIdAndUsedFalseAndRevokedFalseOrderByOriginalLoginAtAsc(UUID userId);
    // -------------------------------------------------------------------------
}
