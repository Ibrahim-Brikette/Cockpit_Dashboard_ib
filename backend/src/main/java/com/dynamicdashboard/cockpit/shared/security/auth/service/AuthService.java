package com.dynamicdashboard.cockpit.shared.security.auth.service;

import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.identity.repository.RoleRepository;
import com.dynamicdashboard.cockpit.identity.repository.UserAccountRepository;
import com.dynamicdashboard.cockpit.shared.security.CustomUserDetails;
import com.dynamicdashboard.cockpit.shared.security.auth.dto.LoginRequest;
import com.dynamicdashboard.cockpit.shared.security.auth.dto.LoginResponse;
import com.dynamicdashboard.cockpit.shared.security.auth.entity.RefreshTokenEntity;
import com.dynamicdashboard.cockpit.shared.security.auth.repository.RefreshTokenRepository;
import com.dynamicdashboard.cockpit.shared.security.jwt.JwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cockpit.auth.mode", havingValue = "STANDALONE", matchIfMissing = true)
public class AuthService {

    // ---- Session policy constants (spec 5.2) --------------------------------

    /** Max active sessions per user before the oldest is evicted on new login. */
    static final int  MAX_CONCURRENT_SESSIONS = 3;

    /**
     * Idle timeout: 30 min without a /refresh call → session dead → must log in again.
     * Works because access token TTL (15 min) < idle timeout (30 min):
     * an active frontend always calls /refresh before the 30-min window closes.
     */
    static final long IDLE_TIMEOUT_MINUTES   = 30;

    /**
     * Absolute hard cap measured from the ORIGINAL login time (spec 5.2).
     * A user who is actively refreshing every 15 min still hits this wall at 8h.
     */
    static final long ABSOLUTE_TIMEOUT_HOURS = 8;

    /** Hard cap on how far the sliding refresh expiry can reach from original login. */
    static final long REFRESH_MAX_DAYS       = 7;

    // ---- Cookie constants ---------------------------------------------------

    static final String REFRESH_COOKIE_NAME = "refresh_token";

    /**
     * Scoped to /api/auth so the browser attaches the cookie only to /refresh
     * and /logout — not to every API call.
     */
    static final String REFRESH_COOKIE_PATH = "/api/auth";

    // ---- Secure random — one instance, reused across all calls --------------

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ---- Dependencies -------------------------------------------------------

    private final AuthenticationManager  authenticationManager;
    private final UserAccountRepository  userAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RoleRepository         roleRepository;
    private final JwtEncoder             jwtEncoder;
    private final JwtProperties          jwtProperties;
    private final JtiRevocationService   jtiRevocationService;

    // =========================================================================
    // POST /api/auth/login
    // =========================================================================

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {

        // 1. Verify credentials — throws BadCredentialsException / LockedException on failure
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        CustomUserDetails details = (CustomUserDetails) auth.getPrincipal();
        UserAccountEntity user    = details.getUserAccountEntity();
        UUID userId               = user.getId();

        // 2. Enforce concurrent-session limit (spec 5.2: max 3 active sessions per user)
        enforceSessionLimit(userId);

        // 3. Generate a session grouping key — plain UUID, not a FK, not a secret.
        //    Every DB operation that targets this session uses this ID.
        UUID sessionId = UUID.randomUUID();

        // 4. Generate the raw refresh token and persist only its SHA-256 hash.
        //    The raw token leaves the server exactly once: inside the HttpOnly cookie.
        //    It is never logged, never stored, never put in the response body.
        Instant loginAt      = Instant.now();
        String  rawToken     = generateRawToken();
        Instant refreshExpiry = computeRefreshExpiry(loginAt);

        RefreshTokenEntity tokenEntity = new RefreshTokenEntity();
        tokenEntity.setTokenHash(sha256(rawToken));
        tokenEntity.setSessionId(sessionId);
        tokenEntity.setUserId(userId);
        tokenEntity.setExpiresAt(refreshExpiry);
        tokenEntity.setOriginalLoginAt(loginAt);  // anchor for the 8h absolute-timeout check
        tokenEntity.setLastUsedAt(loginAt);        // login itself counts as first use
        refreshTokenRepository.save(tokenEntity);

        // 5. Issue the access token with all spec-required claims
        String accessToken = buildAccessToken(userId, sessionId, user.getTenantId());

        // 6. Record last login on the user account
        user.setLastLoginAt(loginAt);
        userAccountRepository.save(user);

        // 7. Deliver the raw token as an HttpOnly cookie — NEVER in the response body (spec 5.2)
        int maxAgeSeconds = (int) Duration.between(loginAt, refreshExpiry).getSeconds();
        setRefreshCookie(response, rawToken, maxAgeSeconds);

        log.info("Login: userId={} sessionId={}", userId, sessionId);
        return new LoginResponse(accessToken, jwtProperties.getAccessTokenTtlMinutes() * 60L);
    }

    // =========================================================================
    // POST /api/auth/refresh
    // =========================================================================

    @Transactional
    public LoginResponse refresh(String rawToken, HttpServletResponse response) {
        // rawToken is extracted by @CookieValue in AuthController — Spring returns 400
        // automatically if the cookie is absent, so no null check needed here.

        // 1. Hash-to-hash lookup — the raw token is never stored or compared directly
        RefreshTokenEntity token = refreshTokenRepository
            .findByTokenHash(sha256(rawToken))
            .orElseThrow(() -> new SecurityException("Invalid refresh token"));

        // 3. Revocation check
        if (token.isRevoked()) {
            throw new SecurityException("Refresh token has been revoked");
        }

        Instant now = Instant.now();

        // 4. Idle-timeout check (spec 5.2: 30 min of inactivity → must re-authenticate).
        //    lastUsedAt is updated on every /refresh so it tracks real activity time.
        Instant idleDeadline = token.getLastUsedAt().plus(IDLE_TIMEOUT_MINUTES, ChronoUnit.MINUTES);
        if (now.isAfter(idleDeadline)) {
            log.info("Idle timeout: userId={} sessionId={}", token.getUserId(), token.getSessionId());
            revokeSession(token.getSessionId());
            throw new SecurityException("Session expired: 30-minute idle limit reached");
        }

        // 5. Absolute-timeout check (spec 5.2: hard 8h cap from ORIGINAL login time).
        //    originalLoginAt never changes — it is the real login moment regardless of
        //    how many /refresh calls have happened since.
        Instant absoluteDeadline = token.getOriginalLoginAt().plus(ABSOLUTE_TIMEOUT_HOURS, ChronoUnit.HOURS);
        if (now.isAfter(absoluteDeadline)) {
            log.info("Absolute timeout: userId={} sessionId={}", token.getUserId(), token.getSessionId());
            revokeSession(token.getSessionId());
            throw new SecurityException("Session expired: 8-hour maximum session duration reached");
        }

        // 6. Update activity timestamp — the only DB write on a normal /refresh call.
        //    No new token row, no new cookie — the same token lives for the session lifetime.
        token.setLastUsedAt(now);
        refreshTokenRepository.save(token);

        // 7. Issue a new access token (the refresh token itself does not change)
        UserAccountEntity user = userAccountRepository.findById(token.getUserId())
            .orElseThrow(() -> new SecurityException("User account not found"));
        String newAccessToken = buildAccessToken(token.getUserId(), token.getSessionId(), user.getTenantId());

        // Cookie is NOT replaced — same raw token remains valid for the session lifetime.

        log.info("Token refreshed: userId={} sessionId={}", token.getUserId(), token.getSessionId());
        return new LoginResponse(newAccessToken, jwtProperties.getAccessTokenTtlMinutes() * 60L);

        // ---------------------------------------------------------------------
        // OLD ROTATION DESIGN — commented, not deleted
        // ---------------------------------------------------------------------
        // Under rotation every /refresh call:
        //   - marked this token as used=true
        //   - generated a brand-new raw token
        //   - inserted a new RefreshTokenEntity row
        //   - replaced the cookie with the new raw token
        // That produced 32 rows and 32 Set-Cookie headers over an 8h session.
        // Removed because the HttpOnly cookie already prevents token theft,
        // making the reuse-detection benefit of rotation negligible.
        //
        // // mark old token as burned
        // token.setUsed(true);
        // refreshTokenRepository.save(token);
        //
        // // generate replacement
        // String newRawToken = generateRawToken();
        // Instant newExpiry  = computeRefreshExpiry(token.getOriginalLoginAt());
        // RefreshTokenEntity newToken = new RefreshTokenEntity();
        // newToken.setTokenHash(sha256(newRawToken));
        // newToken.setSessionId(token.getSessionId());
        // newToken.setUserId(token.getUserId());
        // newToken.setExpiresAt(newExpiry);
        // newToken.setOriginalLoginAt(token.getOriginalLoginAt());
        // newToken.setLastUsedAt(now);
        // refreshTokenRepository.save(newToken);
        //
        // int newMaxAge = (int) Duration.between(now, newExpiry).getSeconds();
        // setRefreshCookie(response, newRawToken, newMaxAge);
        // ---------------------------------------------------------------------
    }

    // =========================================================================
    // POST /api/auth/logout
    // =========================================================================

    @Transactional
    public void logout(Authentication authentication, HttpServletResponse response) {

        // 1. Blacklist the current access token's jti so it cannot be reused before it expires
        Jwt    jwt = ((JwtAuthenticationToken) authentication).getToken();
        String jti = jwt.getId();
        jtiRevocationService.revoke(jti, jwt.getExpiresAt());

        // 2. Delete the session row — user explicitly logged out, no audit row needed
        String sessionIdClaim = jwt.getClaimAsString("sessionId");
        if (sessionIdClaim != null) {
            try {
                deleteSession(UUID.fromString(sessionIdClaim)); // delete, not revoke
            } catch (IllegalArgumentException e) {
                log.warn("Logout: sessionId claim is not a valid UUID: {}", sessionIdClaim);
            }
        }

        // 3. Delete the cookie on the client side
        clearRefreshCookie(response);
        log.info("Logout: jti={} blacklisted", jti);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Enforces the max-3-concurrent-sessions rule (spec 5.2).
     * If the user already has 3 active sessions, the oldest one is revoked first.
     */
    private void enforceSessionLimit(UUID userId) {
        long active = refreshTokenRepository.countByUserIdAndRevokedFalse(userId);
        if (active >= MAX_CONCURRENT_SESSIONS) {
            List<RefreshTokenEntity> oldest = refreshTokenRepository
                .findByUserIdAndRevokedFalseOrderByOriginalLoginAtAsc(userId);
            long toEvict = active - MAX_CONCURRENT_SESSIONS + 1;
            oldest.stream().limit(toEvict).forEach(t -> {
                log.info("Session limit: evicting oldest sessionId={} userId={}",
                         t.getSessionId(), userId);
                deleteSession(t.getSessionId()); // clean eviction — delete, not revoke
            });
        }
    }

    /**
     * Deletes the session row permanently.
     * Used for logout and session eviction — no audit row needed,
     * and a missing row already means "rejected" in the /refresh orElseThrow.
     *
     * OLD: revokeSession() (UPDATE SET revoked=true) was called here too.
     * Changed to DELETE because keeping a dead row after an explicit logout
     * or clean eviction accumulates garbage with zero benefit.
     */
    private void deleteSession(UUID sessionId) {
        int count = refreshTokenRepository.deleteBySessionId(sessionId);
        log.debug("Deleted {} token(s) for sessionId={}", count, sessionId);
    }

    /**
     * Marks the session revoked but keeps the row for audit.
     * Used for idle-timeout and absolute-timeout — session died mid-use,
     * not by explicit user action, so the record is worth keeping.
     */
    private void revokeSession(UUID sessionId) {
        int count = refreshTokenRepository.revokeAllBySessionId(sessionId);
        log.debug("Revoked {} token(s) for sessionId={}", count, sessionId);
    }

    /**
     * Builds a signed RS256 access token with all spec-required claims (spec 5.2):
     * sub, iss, aud, iat, exp, jti, roles, tenantId, sessionId.
     * No "permissions" claim — resolved from DB per-request by JwtAuthoritiesConverter.
     */
    private String buildAccessToken(UUID userId, UUID sessionId, UUID tenantId) {
        List<String> roles = roleRepository.findPermissionsByUserId(userId);
        Instant now        = Instant.now();
        Instant exp        = now.plusSeconds(jwtProperties.getAccessTokenTtlMinutes() * 60L);

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(jwtProperties.getIssuer())
            .audience(List.of(jwtProperties.getAudience()))
            .subject(userId.toString())
            .issuedAt(now)
            .expiresAt(exp)
            .id(UUID.randomUUID().toString())          // jti — unique ID for this token
            .claim("roles",     roles)                  // spec 5.2 required claim
            .claim("tenantId",  tenantId.toString())    // spec 5.2 required claim
            .claim("sessionId", sessionId.toString())   // spec 5.2 required claim
            // No "permissions" claim — JwtAuthoritiesConverter resolves these from DB
            .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * Sliding 8h expiry from now, hard-capped at REFRESH_MAX_DAYS from original login.
     * Ensures the token can never outlive the 7-day absolute maximum.
     */
    private Instant computeRefreshExpiry(Instant originalLoginAt) {
        Instant sliding = Instant.now().plusSeconds(jwtProperties.getRefreshTokenTtlHours() * 3600L);
        Instant hardCap = originalLoginAt.plus(REFRESH_MAX_DAYS, ChronoUnit.DAYS);
        return sliding.isBefore(hardCap) ? sliding : hardCap;
    }

    /**
     * Generates a cryptographically secure opaque token.
     * 32 bytes = 256 bits from SecureRandom → Base64url (no padding) = 43-char string.
     * Used only at login — not on /refresh (no rotation in current design).
     */
    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 of the raw token string.
     * Only this digest is stored in the DB — the raw token is never persisted (spec 5.2).
     */
    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Writes the refresh token as an HttpOnly + Secure + SameSite=Strict cookie.
     * The Set-Cookie header is written manually because the Servlet Cookie API
     * has no method for SameSite — using addCookie() would produce a duplicate header.
     * Called only at login — not on /refresh (cookie does not change mid-session).
     */
    private void setRefreshCookie(HttpServletResponse response, String rawToken, int maxAgeSeconds) {
        String header = REFRESH_COOKIE_NAME + "=" + rawToken
            + "; Path=" + REFRESH_COOKIE_PATH
            + "; HttpOnly"
            + "; Secure"
            + "; SameSite=Strict"
            + "; Max-Age=" + maxAgeSeconds;
        response.addHeader("Set-Cookie", header);
    }

    /** Instructs the browser to delete the cookie by setting Max-Age=0. */
    private void clearRefreshCookie(HttpServletResponse response) {
        String header = REFRESH_COOKIE_NAME + "="
            + "; Path=" + REFRESH_COOKIE_PATH
            + "; HttpOnly"
            + "; Secure"
            + "; SameSite=Strict"
            + "; Max-Age=0";
        response.addHeader("Set-Cookie", header);
    }

    // -------------------------------------------------------------------------
    // OLD MANUAL COOKIE EXTRACTION — commented, not deleted
    // -------------------------------------------------------------------------
    // Replaced by @CookieValue("refresh_token") in AuthController.
    // Spring extracts the cookie value and passes it directly as a method parameter,
    // returning 400 automatically if the cookie is absent — cleaner and safer.
    //
    // private String extractRefreshCookie(HttpServletRequest request) {
    //     Cookie[] cookies = request.getCookies();
    //     if (cookies == null) return null;
    //     return Arrays.stream(cookies)
    //         .filter(c -> REFRESH_COOKIE_NAME.equals(c.getName()))
    //         .map(Cookie::getValue)
    //         .findFirst()
    //         .orElse(null);
    // }
    // -------------------------------------------------------------------------
}
