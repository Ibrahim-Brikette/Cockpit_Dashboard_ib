package com.dynamicdashboard.cockpit.shared.security.auth.service;

import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.identity.repository.UserAccountRepository;
import com.dynamicdashboard.cockpit.shared.security.MailService;
import com.dynamicdashboard.cockpit.shared.security.auth.entity.PasswordResetTokenEntity;
import com.dynamicdashboard.cockpit.shared.security.auth.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vertical slice: brute-force protection for STANDALONE mode.
 *
 * Two independent layers — neither knows the other's decision:
 *
 *   Layer 1 — IP throttle (in-memory, no DB)
 *     Blocks after IP_MAX_ATTEMPTS failures within a IP_WINDOW_MINUTES sliding window.
 *     Stops bots and credential stuffing from a single IP.
 *     Resets on server restart (acceptable — it is a rate limiter, not an audit log).
 *
 *   Layer 2 — Account lockout (DB)
 *     Blocks after MAX_FAILED_ATTEMPTS failures against the same account, regardless of IP.
 *     Escalates duration on each lockout so a patient attacker (botnet, different IPs)
 *     eventually hits a permanent lock. lockout_count survives auto-expiry — the attacker
 *     cannot erase history simply by waiting.
 *     Triggers a 5-minute password-reset link sent to the account owner's email.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cockpit.auth.mode", havingValue = "STANDALONE", matchIfMissing = true)
public class BruteForceService {

    // ---- Account lockout constants ----------------------------------------
    static final int  MAX_FAILED_ATTEMPTS         = 5;
    static final int  PERMANENT_LOCKOUT_THRESHOLD = 4;   // lockout_count >= 4 → permanent
    static final long RESET_TOKEN_TTL_SECONDS     = 300; // 5 min

    // ---- IP throttle constants --------------------------------------------
    static final int  IP_MAX_ATTEMPTS   = 10;
    static final long IP_WINDOW_MINUTES = 15;

    // In-memory: IP → [attemptCount, windowStartEpochSecond]
    private final Map<String, long[]> ipAttempts  = new ConcurrentHashMap<>();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserAccountRepository        userAccountRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailService                  mailService;

    // ======================================================================
    // Layer 1 — IP throttle
    // ======================================================================

    /**
     * Throws IpThrottledException if this IP has exceeded IP_MAX_ATTEMPTS
     * within the current IP_WINDOW_MINUTES window.
     * Reads only the in-memory map — no DB access, runs before any username lookup.
     */
    public void checkIpThrottle(String ip) {
        long[] slot = ipAttempts.get(ip);
        if (slot == null) return;
        long now = Instant.now().getEpochSecond();
        if (now > slot[1] + (IP_WINDOW_MINUTES * 60)) { ipAttempts.remove(ip); return; }
        if (slot[0] >= IP_MAX_ATTEMPTS) throw new IpThrottledException();
    }

    private void incrementIpAttempt(String ip) {
        long now = Instant.now().getEpochSecond();
        ipAttempts.compute(ip, (k, slot) -> {
            if (slot == null || now > slot[1] + (IP_WINDOW_MINUTES * 60)) return new long[]{1, now};
            slot[0]++;
            return slot;
        });
    }

    /** Removes expired IP windows every 5 minutes so the map never grows unbounded. */
    @Scheduled(fixedRate = 300_000)
    public void cleanExpiredIpWindows() {
        long now = Instant.now().getEpochSecond();
        ipAttempts.entrySet().removeIf(e -> now > e.getValue()[1] + (IP_WINDOW_MINUTES * 60));
    }

    // ======================================================================
    // Layer 2 — Account lockout
    // ======================================================================

    /**
     * Checks if the account is currently locked.
     * If a timed lock has auto-expired, clears locked_until so the attempt proceeds —
     * but lockout_count stays incremented (attacker cannot erase escalation history by waiting).
     * If username is unknown: returns silently — never reveals whether the account exists.
     */
    @Transactional
    public void checkAccountLock(String username) {
        UserAccountEntity user = userAccountRepository.findByUsername(username).orElse(null);
        if (user == null) return;

        if (user.isPermanentlyLocked())
            throw new AccountLockedException("Account is permanently locked. Check your email to reset your password.");

        if (user.getLockedUntil() != null) {
            if (Instant.now().isBefore(user.getLockedUntil()))
                throw new AccountLockedException("Account is temporarily locked. Check your email to reset your password.");
            // Auto-expiry: window passed — unlock the account, keep lockout_count
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userAccountRepository.save(user);
        }
    }

    /**
     * Called on successful login.
     * Clears the IP slot and resets all account lockout state.
     * lockout_count is also reset — the user authenticated, so the escalation ladder resets.
     */
    @Transactional
    public void onLoginSuccess(String username, String ip) {
        ipAttempts.remove(ip);
        userAccountRepository.findByUsername(username).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            user.setLockoutCount(0);
            user.setPermanentlyLocked(false);
            userAccountRepository.save(user);
        });
    }

    /**
     * Called on failed login (wrong password).
     * Increments the IP counter (Layer 1) and the account counter (Layer 2).
     * Triggers a lockout if the account threshold is reached.
     */
    @Transactional
    public void onLoginFailure(String username, String ip) {
        incrementIpAttempt(ip);

        UserAccountEntity user = userAccountRepository.findByUsername(username).orElse(null);
        if (user == null) return; // IP counter already incremented — nothing else to do

        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            triggerLockout(user);
        } else {
            userAccountRepository.save(user);
            log.info("Failed login: userId={} attempts={}/{}", user.getId(), attempts, MAX_FAILED_ATTEMPTS);
        }
    }

    // ======================================================================
    // Private — lockout mechanics
    // ======================================================================

    private void triggerLockout(UserAccountEntity user) {
        int newCount  = user.getLockoutCount() + 1;
        boolean permanent = newCount >= PERMANENT_LOCKOUT_THRESHOLD;

        user.setLockoutCount(newCount);
        user.setFailedLoginAttempts(0); // reset for next window

        String durationText;
        if (permanent) {
            user.setPermanentlyLocked(true);
            durationText = "permanently";
        } else {
            Duration d = lockDuration(newCount);
            user.setLockedUntil(Instant.now().plus(d));
            durationText = formatDuration(d);
        }
        userAccountRepository.save(user);

        // Raw token → email link. Hash → DB. Raw value never persisted.
        String rawToken = generateRawToken();
        PasswordResetTokenEntity resetToken = new PasswordResetTokenEntity();
        resetToken.setTokenHash(sha256(rawToken));
        resetToken.setUserId(user.getId());
        resetToken.setExpiresAt(Instant.now().plusSeconds(RESET_TOKEN_TTL_SECONDS));
        resetToken.setUsed(false);
        passwordResetTokenRepository.save(resetToken);

        mailService.sendLockoutEmail(user.getEmail(), user.getDisplayName(), rawToken, durationText, permanent);
        log.warn("Account locked: userId={} lockoutCount={} permanent={}", user.getId(), newCount, permanent);
    }

    private Duration lockDuration(int lockoutCount) {
        return switch (lockoutCount) {
            case 1 -> Duration.ofMinutes(30);
            case 2 -> Duration.ofHours(2);
            case 3 -> Duration.ofHours(8);
            default -> Duration.ofHours(8); // unreachable — permanent branch taken at count >= 4
        };
    }

    private String formatDuration(Duration d) {
        if (d.toHours() >= 1) return d.toHours() + (d.toHours() == 1 ? " hour" : " hours");
        return d.toMinutes() + (d.toMinutes() == 1 ? " minute" : " minutes");
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
