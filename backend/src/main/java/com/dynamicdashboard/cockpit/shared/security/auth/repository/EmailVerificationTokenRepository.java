package com.dynamicdashboard.cockpit.shared.security.auth.repository;

import com.dynamicdashboard.cockpit.shared.security.auth.entity.EmailVerificationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationTokenEntity, UUID> {

    /**
     * Hash-to-hash lookup used by VerifyEmailService.
     * The caller hashes the raw token from the request body before calling this.
     * Raw token never travels further than the controller — only the digest hits the DB.
     */
    Optional<EmailVerificationTokenEntity> findByTokenHash(String tokenHash);
}
