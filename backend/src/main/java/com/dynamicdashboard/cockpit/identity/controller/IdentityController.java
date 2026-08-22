package com.dynamicdashboard.cockpit.identity.controller;

import com.dynamicdashboard.cockpit.identity.application.IdentityApplicationService;
import com.dynamicdashboard.cockpit.identity.application.UserDeletionBlockedException;
import com.dynamicdashboard.cockpit.identity.application.dto.*;
import com.dynamicdashboard.cockpit.shared.security.CockpitAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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

    /**
     * Injected to guard the createUser endpoint at runtime.
     * @ConditionalOnProperty cannot be placed on a single method — only on beans.
     * IdentityController must stay active in both modes for the GET endpoints.
     * So we check the mode inside the method and return 404 in INTEGRATED mode.
     */
    private final CockpitAuthProperties cockpitAuthProperties;

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
     *   - ROLE_TENANT_ADMIN or ROLE_SUPER_ADMIN  → any profile → allowed
     *
     * Returns 403 if neither condition is met.
     * Returns 404 if the user does not exist (only reachable by admins targeting other users).
     * Role names come from AppRole — update both if a role is renamed.
     */
    @GetMapping("/{id}")
    @PreAuthorize("#id.toString() == authentication.name or hasRole('TENANT_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserAccountDto> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(identityApplicationService.getUserById(id));
    }


    @PreAuthorize("hasRole('TENANT_ADMIN') or hasRole('SUPER_ADMIN') " +
                  "or hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.DashboardPermission).SHARE.code) " +
                  "or hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).SHARE.code)")
    @GetMapping("/users")
    public ResponseEntity<List<UserAccountDto>> getAllUsers() {
        return ResponseEntity.ok(identityApplicationService.getAllUsers());
    }

    /**
     * POST /api/identity/users — STANDALONE mode only, admin role required.
     *
     * Creates a new user account with status=PENDING and sends an activation email.
     * The user sets their own password by clicking the link in the email
     * and calling POST /api/auth/verify-email.
     *
     * Returns 404 in INTEGRATED mode — user management belongs to the mother app there.
     * Returns 201 with the created user DTO (no password field) on success.
     * Returns 400 if username or email is already taken.
     */
    @PostMapping("/users")
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserAccountDto> createUser(@RequestBody CreateUserRequest request) {

        // Runtime guard: user creation only makes sense when Cockpit owns the identity store
        if (cockpitAuthProperties.getMode() != CockpitAuthProperties.AuthMode.STANDALONE) {
            return ResponseEntity.notFound().build();
        }

        try {
            UserAccountDto created = identityApplicationService.createUser(request);
            return ResponseEntity.status(201).body(created);
        } catch (IllegalArgumentException e) {
            // Username or email already taken — return 400, message is safe to expose
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * DELETE /api/identity/users/{id} — STANDALONE mode only, admin role required.
     *
     * Permanently removes the user account.
     * Returns 204 on success.
     * Returns 400 if the caller attempts to delete their own account.
     * Returns 404 if the user does not exist or if called in INTEGRATED mode
     *   (user management belongs to the mother app in that mode).
     */
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        if (cockpitAuthProperties.getMode() != CockpitAuthProperties.AuthMode.STANDALONE) {
            return ResponseEntity.notFound().build();
        }
        try {
            identityApplicationService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (UserDeletionBlockedException e) {
            // Structured 409 — the frontend renders a specific message with the counts
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("errorCode", "USER_HAS_OWNED_RESOURCES");
            body.put("dashboardCount", e.getDashboardCount());
            body.put("queryCount", e.getQueryCount());
            body.put("message", buildBlockedMessage(e.getDashboardCount(), e.getQueryCount()));
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        } catch (DataIntegrityViolationException e) {
            // Safety net — should not happen after the cleanup above, but guard anyway
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("errorCode", "FK_CONSTRAINT_VIOLATION");
            body.put("message", "Impossible de supprimer l'utilisateur : des données liées existent encore.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }
    }

    private String buildBlockedMessage(long dashboards, long queries) {
        StringBuilder sb = new StringBuilder("Impossible de supprimer cet utilisateur : il possède ");
        if (dashboards > 0) {
            sb.append(dashboards).append(dashboards == 1 ? " tableau de bord" : " tableaux de bord");
        }
        if (dashboards > 0 && queries > 0) {
            sb.append(" et ");
        }
        if (queries > 0) {
            sb.append(queries).append(queries == 1 ? " requête" : " requêtes");
        }
        sb.append(". Veuillez les réassigner ou les supprimer avant de supprimer l'utilisateur.");
        return sb.toString();
    }

    // =========================================================================
    // ROLES
    // =========================================================================

    /**
     * GET /api/identity/roles
     * Returns the full list of seeded roles. Read-only — roles are managed by the
     * seeder, not created ad hoc through the API.
     */
    @GetMapping("/roles")
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<RoleDto>> getRoles() {
        return ResponseEntity.ok(identityApplicationService.getRoles());
    }

    /**
     * GET /api/identity/users/{userId}/roles
     * A user may fetch their own assignments; admins may fetch any user's.
     */
    @GetMapping("/users/{userId}/roles")
    @PreAuthorize("#userId.toString() == authentication.name or hasRole('TENANT_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UserRoleDto>> getUserRoles(@PathVariable UUID userId) {
        return ResponseEntity.ok(identityApplicationService.getUserRoles(userId));
    }

    /**
     * POST /api/identity/users/{userId}/roles — STANDALONE mode only.
     * Body: { roleId, assignmentScope }
     * Returns 201 with the created assignment. Returns 400 if already assigned.
     */
    @PostMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserRoleDto> assignRole(
            @PathVariable UUID userId,
            @RequestBody AssignRoleRequest request) {

        if (cockpitAuthProperties.getMode() != CockpitAuthProperties.AuthMode.STANDALONE) {
            return ResponseEntity.notFound().build();
        }
        try {
            return ResponseEntity.status(201).body(identityApplicationService.assignRole(userId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * DELETE /api/identity/users/{userId}/roles/{roleId} — STANDALONE mode only.
     * Returns 204 on success, 404 if the assignment does not exist.
     */
    @DeleteMapping("/users/{userId}/roles/{roleId}")
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> removeRole(
            @PathVariable UUID userId,
            @PathVariable UUID roleId) {

        if (cockpitAuthProperties.getMode() != CockpitAuthProperties.AuthMode.STANDALONE) {
            return ResponseEntity.notFound().build();
        }
        try {
            identityApplicationService.removeRole(userId, roleId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // GROUPS
    // =========================================================================

    /**
     * GET /api/identity/users/{userId}/groups
     * Returns the groups this user is a member of (user-centric view).
     * Own profile or admin required — same rule as getUserRoles.
     */
    @GetMapping("/users/{userId}/groups")
    @PreAuthorize("#userId.toString() == authentication.name or hasRole('TENANT_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UserGroupDto>> getUserGroups(@PathVariable UUID userId) {
        return ResponseEntity.ok(identityApplicationService.getUserGroups(userId));
    }

    /** GET /api/identity/groups */
    @GetMapping("/groups")
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasRole('SUPER_ADMIN') " +
                  "or hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.DashboardPermission).SHARE.code) " +
                  "or hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).SHARE.code)")
    public ResponseEntity<List<GroupDto>> getGroups() {
        return ResponseEntity.ok(identityApplicationService.getGroups());
    }

    /**
     * POST /api/identity/groups — STANDALONE mode only.
     * Returns 201 with the created group. Returns 400 if group code already exists.
     */
    @PostMapping("/groups")
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<GroupDto> createGroup(@RequestBody CreateGroupRequest request) {
        if (cockpitAuthProperties.getMode() != CockpitAuthProperties.AuthMode.STANDALONE) {
            return ResponseEntity.notFound().build();
        }
        try {
            return ResponseEntity.status(201).body(identityApplicationService.createGroup(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * DELETE /api/identity/groups/{groupId} — STANDALONE mode only, admin role required.
     * Removes the group and cascades to memberships and share grants.
     * Returns 204 on success, 404 if the group does not exist.
     */
    @DeleteMapping("/groups/{groupId}")
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID groupId) {
        if (cockpitAuthProperties.getMode() != CockpitAuthProperties.AuthMode.STANDALONE) {
            return ResponseEntity.notFound().build();
        }
        try {
            identityApplicationService.deleteGroup(groupId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** GET /api/identity/groups/{groupId}/members */
    @GetMapping("/groups/{groupId}/members")
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<GroupMemberDto>> getGroupMembers(@PathVariable UUID groupId) {
        return ResponseEntity.ok(identityApplicationService.getGroupMembers(groupId));
    }

    /**
     * POST /api/identity/groups/{groupId}/members — STANDALONE mode only.
     * Body: { userId, membershipRole }
     * Returns 201 with the created membership. Returns 400 if already a member.
     */
    @PostMapping("/groups/{groupId}/members")
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<GroupMemberDto> addGroupMember(
            @PathVariable UUID groupId,
            @RequestBody AddGroupMemberRequest request) {

        if (cockpitAuthProperties.getMode() != CockpitAuthProperties.AuthMode.STANDALONE) {
            return ResponseEntity.notFound().build();
        }
        try {
            return ResponseEntity.status(201).body(identityApplicationService.addGroupMember(groupId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * DELETE /api/identity/groups/{groupId}/members/{userId} — STANDALONE mode only.
     * Returns 204 on success, 404 if the membership does not exist.
     */
    @DeleteMapping("/groups/{groupId}/members/{userId}")
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> removeGroupMember(
            @PathVariable UUID groupId,
            @PathVariable UUID userId) {

        if (cockpitAuthProperties.getMode() != CockpitAuthProperties.AuthMode.STANDALONE) {
            return ResponseEntity.notFound().build();
        }
        try {
            identityApplicationService.removeGroupMember(groupId, userId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
