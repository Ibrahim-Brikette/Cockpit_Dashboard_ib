package com.dynamicdashboard.cockpit.sharing.controller;

import com.dynamicdashboard.cockpit.sharing.application.SharingApplicationService;
import com.dynamicdashboard.cockpit.sharing.application.dto.CreateShareGrantRequest;
import com.dynamicdashboard.cockpit.sharing.application.dto.ShareGrantDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Share grant endpoints — available in both STANDALONE and INTEGRATED modes.
 *
 * Access rules:
 *   - Reading grants: the resource owner, or TENANT_ADMIN / SUPER_ADMIN.
 *   - Creating / deleting grants: same as reading.
 *
 * Resource ownership is not checked here (would require a separate join);
 * the evaluator in permission_evaluators handles the detailed owner/share check.
 * These endpoints therefore require at minimum the admin role for writes,
 * which is safe — admins can see and manage all grants.
 *
 * For a future iteration, a @PostAuthorize or custom evaluator can restrict
 * non-admin users to only managing grants on resources they own.
 */
@RestController
@RequestMapping("/api/sharing")
@RequiredArgsConstructor
public class SharingController {

    private final SharingApplicationService sharingApplicationService;

    // =========================================================================
    // DASHBOARD GRANTS
    // =========================================================================

    /** GET /api/sharing/dashboards/{id}/grants */
    @GetMapping("/dashboards/{id}/grants")
    @PreAuthorize("hasPermission(#id, 'Dashboard', T(com.dynamicdashboard.cockpit.shared.security.authorization.DashboardPermission).VIEW.code)")
    public ResponseEntity<List<ShareGrantDto>> getDashboardGrants(@PathVariable UUID id) {
        return ResponseEntity.ok(sharingApplicationService.getDashboardGrants(id));
    }

    /**
     * POST /api/sharing/dashboards/{id}/grants
     * Body: { shareLevel, accessLevel, granteeUserId?, granteeGroupId? }
     * Returns 201 with the created grant.
     */
    @PostMapping("/dashboards/{id}/grants")
    @PreAuthorize("hasPermission(#id, 'Dashboard', T(com.dynamicdashboard.cockpit.shared.security.authorization.DashboardPermission).SHARE.code)")
    public ResponseEntity<ShareGrantDto> createDashboardGrant(
            @PathVariable UUID id,
            @RequestBody CreateShareGrantRequest request) {
        try {
            return ResponseEntity.status(201).body(sharingApplicationService.createDashboardGrant(id, request));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/sharing/dashboards/{id}/grants/{grantId}
     * Returns 204 on success, 404 if the grant does not exist.
     */
    @DeleteMapping("/dashboards/{id}/grants/{grantId}")
    @PreAuthorize("hasPermission(#id, 'Dashboard', T(com.dynamicdashboard.cockpit.shared.security.authorization.DashboardPermission).SHARE.code)")
    public ResponseEntity<Void> removeDashboardGrant(
            @PathVariable UUID id,
            @PathVariable UUID grantId) {
        try {
            sharingApplicationService.removeDashboardGrant(grantId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // QUERY GRANTS
    // =========================================================================

    /** GET /api/sharing/queries/{id}/grants */
    @GetMapping("/queries/{id}/grants")
    @PreAuthorize("hasPermission(#id, 'Query', T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).VIEW.code)")
    public ResponseEntity<List<ShareGrantDto>> getQueryGrants(@PathVariable UUID id) {
        return ResponseEntity.ok(sharingApplicationService.getQueryGrants(id));
    }

    /**
     * POST /api/sharing/queries/{id}/grants
     * Body: { shareLevel, accessLevel, granteeUserId?, granteeGroupId? }
     * Returns 201 with the created grant.
     */
    @PostMapping("/queries/{id}/grants")
    @PreAuthorize("hasPermission(#id, 'Query', T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).SHARE.code)")
    public ResponseEntity<ShareGrantDto> createQueryGrant(
            @PathVariable UUID id,
            @RequestBody CreateShareGrantRequest request) {
        try {
            return ResponseEntity.status(201).body(sharingApplicationService.createQueryGrant(id, request));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/sharing/queries/{id}/grants/{grantId}
     * Returns 204 on success, 404 if the grant does not exist.
     */
    @DeleteMapping("/queries/{id}/grants/{grantId}")
    @PreAuthorize("hasPermission(#id, 'Query', T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).SHARE.code)")
    public ResponseEntity<Void> removeQueryGrant(
            @PathVariable UUID id,
            @PathVariable UUID grantId) {
        try {
            sharingApplicationService.removeQueryGrant(grantId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
