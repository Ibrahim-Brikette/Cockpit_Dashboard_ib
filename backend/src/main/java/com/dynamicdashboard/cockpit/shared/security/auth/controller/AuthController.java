package com.dynamicdashboard.cockpit.shared.security.auth.controller;

import com.dynamicdashboard.cockpit.shared.security.auth.service.AccountLockedException;
import com.dynamicdashboard.cockpit.shared.security.auth.service.BruteForceService;
import com.dynamicdashboard.cockpit.shared.security.auth.service.IpThrottledException;
import com.dynamicdashboard.cockpit.shared.security.auth.dto.LoginRequest;
import com.dynamicdashboard.cockpit.shared.security.auth.dto.LoginResponse;
import com.dynamicdashboard.cockpit.shared.security.auth.dto.ForgotPasswordRequest;
import com.dynamicdashboard.cockpit.shared.security.auth.service.ForgotPasswordService;
import com.dynamicdashboard.cockpit.shared.security.auth.dto.PasswordResetRequest;
import com.dynamicdashboard.cockpit.shared.security.auth.service.PasswordResetService;
import com.dynamicdashboard.cockpit.shared.security.auth.dto.VerifyEmailRequest;
import com.dynamicdashboard.cockpit.shared.security.auth.service.VerifyEmailService;
import com.dynamicdashboard.cockpit.shared.security.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Entry point for all STANDALONE-mode authentication endpoints.
 *
 * This controller is intentionally thin — each method delegates immediately
 * to its service. No business logic lives here.
 *
 * Endpoints:
 *   POST /api/auth/login            public — credential verification + session creation
 *   POST /api/auth/refresh          public — sliding access-token renewal via HttpOnly cookie
 *   POST /api/auth/logout           protected — session revocation + JTI blacklist
 *   POST /api/auth/forgot-password  public — user-initiated password reset email
 *   POST /api/auth/password-reset   public — consume reset link (lockout or forgot-password), set new password
 *   POST /api/auth/verify-email     public — consume account-setup link, set initial password, activate account
 */
@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(name = "cockpit.auth.mode", havingValue = "STANDALONE", matchIfMissing = true)
@RequiredArgsConstructor
public class AuthController {

    private final AuthService           authService;
    private final BruteForceService     bruteForceService;
    private final PasswordResetService  passwordResetService;
    private final ForgotPasswordService forgotPasswordService;
    private final VerifyEmailService    verifyEmailService;

    /**
     * POST /api/auth/login
     *
     * Two independent protection layers run before authentication:
     *   1. IP throttle  (Layer 1) — in-memory, no DB, returns 429 if exceeded
     *   2. Account lock (Layer 2) — DB read, returns 423 if locked
     * Neither layer knows the other's decision.
     *
     * On wrong password: both layers increment their respective counters.
     * On success: both layers clear their counters for this user/IP.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        String ip = httpRequest.getRemoteAddr();

        // Layer 1 — IP throttle: independent of account state
        try {
            bruteForceService.checkIpThrottle(ip);
        } catch (IpThrottledException e) {
            return ResponseEntity.status(429).build();
        }

        // Layer 2 — Account lock: independent of IP state
        try {
            bruteForceService.checkAccountLock(request.username());
        } catch (AccountLockedException e) {
            return ResponseEntity.status(423).build();
        }

        // Authentication — BadCredentialsException propagates here from AuthService
        try {
            LoginResponse loginResponse = authService.login(request, response);
            bruteForceService.onLoginSuccess(request.username(), ip);
            return ResponseEntity.ok(loginResponse);
        } catch (BadCredentialsException e) {
            bruteForceService.onLoginFailure(request.username(), ip);
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * POST /api/auth/refresh
     *
     * Reads the refresh token from the HttpOnly cookie.
     * Spring returns 400 automatically if the cookie is absent (required = true by default).
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
     * Protected — requires a valid Bearer access token.
     * Blacklists the JTI, revokes the session, clears the cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            Authentication authentication,
            HttpServletResponse response) {
        authService.logout(authentication, response);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/auth/forgot-password
     * Body: { "email": "alice@example.com" }
     *
     * Always returns 204 — never reveals whether the email is registered
     * (prevents email enumeration attacks).
     * If a valid reset token already exists for this user, no new one is created
     * (prevents email bombing — see ForgotPasswordService).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        forgotPasswordService.execute(request.email());
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/auth/password-reset
     * Body: { "token": "...", "newPassword": "..." }
     *
     * Public endpoint — the user may be locked out and have no access token.
     * Shared by two flows that both use the same password_reset_token table:
     *   - Lockout reset  : token sent automatically by BruteForceService when the account locks
     *   - Forgot password: token sent by ForgotPasswordService on user request
     *
     * On success: password updated, all lockout state cleared, token marked used → 204.
     * On failure: 400 (invalid token, expired, already used, weak password).
     */
    @PostMapping("/password-reset")
    public ResponseEntity<Void> passwordReset(@RequestBody PasswordResetRequest request) {
        try {
            passwordResetService.execute(request.token(), request.newPassword());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * POST /api/auth/verify-email
     * Body: { "token": "...", "password": "..." }
     *
     * Public endpoint — the user has no account yet (status=PENDING, no access token).
     * Called when a newly created user clicks the activation link in their invitation email.
     *
     * The token is extracted from the URL query string by the frontend automatically
     * and placed in the request body — the user never types it manually.
     *
     * On success: initial password set, account activated (PENDING → ACTIVE), token marked used → 204.
     *             The user can now log in via POST /api/auth/login.
     * On failure: 400 (invalid token, expired, already used, weak password).
     */
    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestBody VerifyEmailRequest request) {
        try {
            verifyEmailService.execute(request.token(), request.password());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
