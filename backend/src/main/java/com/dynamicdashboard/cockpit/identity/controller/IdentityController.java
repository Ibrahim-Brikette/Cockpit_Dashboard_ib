package com.dynamicdashboard.cockpit.identity.controller;

import com.dynamicdashboard.cockpit.identity.application.IdentityApplicationService;
import com.dynamicdashboard.cockpit.identity.application.dto.UserAccountDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// -------------------------------------------------------------------------
// @CrossOrigin(origins = "*") — removed, not deleted
// -------------------------------------------------------------------------
// Was overriding the global CORS configuration set in SecurityConfiguration.
// Global config already handles allowed origins using the explicit list from
// SecurityProperties and sets allowCredentials=true for cookie support.
// A wildcard @CrossOrigin here would conflict with allowCredentials=true
// (browsers reject wildcard + credentials) and bypass the intentional config.
// -------------------------------------------------------------------------

@RestController
@RequestMapping("/api/identity")
@RequiredArgsConstructor
public class IdentityController {

    private final IdentityApplicationService identityApplicationService;

    // -------------------------------------------------------------------------
    // GET /me — commented, not deleted
    // -------------------------------------------------------------------------
    // Replaced by GET /{id} below. /me was a convenience alias that read the
    // authenticated user without requiring an ID in the path. Now the frontend
    // can call GET /{id} with the userId it already holds from the access token
    // (the sub claim). Keeping both would duplicate the same logic.
    //
    // @GetMapping("/me")
    // public ResponseEntity<UserAccountDto> getCurrentUser() {
    //     return ResponseEntity.ok(identityApplicationService.getCurrentUser());
    // }
    // -------------------------------------------------------------------------

    /**
     * GET /api/identity/{id}
     *
     * Security rule (spec: a user fetches their own profile OR an admin fetches any profile):
     *   - authentication.name = JWT sub = userId UUID
     *   - #id.toString() == authentication.name  → own profile → allowed
     *   - ROLE_TENANT_ADMIN or ROLE_SYSTEM_ADMIN  → any profile → allowed
     *
     * Returns 403 if neither condition is met.
     * Returns 404 if the user does not exist (only reachable by admins targeting other users).
     */
    @GetMapping("/{id}")
    @PreAuthorize("#id.toString() == authentication.name or hasRole('TENANT_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<UserAccountDto> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(identityApplicationService.getUserById(id));
    }

    @PreAuthorize("hasRole('TENANT_ADMIN') or hasRole('SYSTEM_ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<List<UserAccountDto>> getAllUsers() {
        return ResponseEntity.ok(identityApplicationService.getAllUsers());
    }
}
