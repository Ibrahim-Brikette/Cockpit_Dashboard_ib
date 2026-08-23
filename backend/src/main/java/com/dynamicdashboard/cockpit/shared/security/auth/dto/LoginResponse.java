package com.dynamicdashboard.cockpit.shared.security.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Returned by POST /login and POST /refresh.
 *
 * Spec 5.2 storage rules:
 *   - access_token: frontend stores in memory ONLY (never localStorage / sessionStorage).
 *   - refresh token: travels via HttpOnly cookie ONLY — never in this response body.
 */
public record LoginResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type")   String tokenType,
    @JsonProperty("expires_in")   long expiresIn
) {
    /** Convenience constructor — token type is always Bearer. */
    public LoginResponse(String accessToken, long expiresInSeconds) {
        this(accessToken, "Bearer", expiresInSeconds);
    }
}
