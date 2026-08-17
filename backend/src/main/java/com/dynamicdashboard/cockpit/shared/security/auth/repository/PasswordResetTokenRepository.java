package com.dynamicdashboard.cockpit.shared.security.auth.repository;

import com.dynamicdashboard.cockpit.shared.security.auth.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    /** Lookup by hash — used by PasswordResetService to validate an incoming raw token. */
    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    /**
     * Returns true if a valid (not yet used, not yet expired) token already exists for this user.
     * Used by ForgotPasswordService to prevent email bombing:
     * if a valid token exists, no new one is created.
     */
    boolean existsByUserIdAndUsedFalseAndExpiresAtAfter(UUID userId, Instant now);
}
