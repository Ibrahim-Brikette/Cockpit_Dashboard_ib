package com.dynamicdashboard.cockpit.shared.security.auth.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory JTI (JWT ID) blacklist — used to reject access tokens that were
 * explicitly revoked before their natural expiry (logout, compromise response).
 *
 * ⚠ PLACEHOLDER — in-memory only.
 *   On application restart the blacklist is wiped. A logged-out token whose 15-min
 *   window hasn't elapsed yet will be accepted again after restart.
 *   Production requirement (spec 5.2): replace with Redis + TTL-based eviction.
 *   Do NOT replace without confirmation — this in-memory version is intentional for now.
 *
 * Pruning: expired entries are removed lazily on every isRevoked() call, and in bulk
 * by pruneExpired() which can be wired to a @Scheduled task if needed.
 */
@Service
public class JtiRevocationService {

    /** jti → token expiry time. Entries are pruned once past their expiry. */
    private final ConcurrentHashMap<String, Instant> blacklist = new ConcurrentHashMap<>();

    /**
     * Blacklists a JTI until its access token naturally expires.
     * @param jti    the jti claim (UUID v4 string)
     * @param expiry the token's exp claim — used to prune the entry after natural expiry
     */
    public void revoke(String jti, Instant expiry) {
        if (jti != null && expiry != null) {
            blacklist.put(jti, expiry);
        }
    }

    /**
     * Returns true if this JTI is currently blacklisted.
     * Prunes the entry if it has already passed its natural expiry
     * (the decoder's own exp check would reject it anyway).
     */
    public boolean isRevoked(String jti) {
        if (jti == null) return false;
        Instant expiry = blacklist.get(jti);
        if (expiry == null) return false;
        if (Instant.now().isAfter(expiry)) {
            blacklist.remove(jti);   // lazy prune — token expired naturally, no longer needed
            return false;
        }
        return true;
    }

    /**
     * Bulk-removes entries whose tokens have already expired naturally.
     * Call this on a schedule (e.g. every 5 min) to prevent unbounded map growth.
     * Wire with @Scheduled(fixedRate = 300_000) on a @Configuration class that
     * has @EnableScheduling, if needed.
     */
    public void pruneExpired() {
        Instant now = Instant.now();
        blacklist.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }
}
