package com.dynamicdashboard.cockpit.shared.security.auth.controller;

import com.dynamicdashboard.cockpit.shared.security.auth.dto.LoginRequest;
import com.dynamicdashboard.cockpit.shared.security.auth.dto.LoginResponse;
import com.dynamicdashboard.cockpit.shared.security.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(name = "cockpit.auth.mode", havingValue = "STANDALONE", matchIfMissing = true)
//cockpit.auth.mode = STANDALONE   →  havingValue matches  →  bean LOADS
//cockpit.auth.mode = INTEGRATED   →  havingValue no match →  bean SKIPPED  ✓
//cockpit.auth.mode = (not set)    →  matchIfMissing=true  →  bean LOADS (safe default)

@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/login
     *
     * Verifies credentials, creates a session, issues:
     *   - access token  (RS256, 15 min) in the JSON response body
     *   - refresh token (opaque UUID)   as an HttpOnly/Secure/SameSite=Strict cookie
     * Enforces the max-3-concurrent-sessions limit before issuing.
     * Public endpoint — no Bearer token required.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response) {

        return ResponseEntity.ok(authService.login(request, response));
    }

    /**
     * POST /api/auth/refresh
     *
     * Reads the refresh token from the HttpOnly cookie via @CookieValue.
     * Spring extracts it automatically — if the cookie is absent it returns 400
     * before this method runs (required = true by default).
     * On success: issues a new access token in the response body.
     * On failure (expired, revoked, timeout): returns 401.
     * Public endpoint — access token may already be expired when this is called.
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue("refresh_token") String rawToken,
            HttpServletResponse response) {

        return ResponseEntity.ok(authService.refresh(rawToken, response));
    }

    /**
     * POST /api/auth/logout
     *
     * Requires a valid access token (protected — standard Bearer auth applies).
     * On success:
     *   - Blacklists the current access token's jti (spec 5.2: server-side revocation)
     *   - Revokes all refresh tokens for this session
     *   - Clears the HttpOnly cookie (Max-Age=0)
     * The frontend must also clear the access token from memory.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            Authentication authentication,
            HttpServletResponse response) {

        authService.logout(authentication, response);
        return ResponseEntity.noContent().build();
    }
}
