package com.dynamicdashboard.cockpit.identity.application.dto;

import java.util.UUID;

/**
 * Request body for POST /api/identity/users (STANDALONE mode, admin only).
 *
 * username    — unique login name. Validated for uniqueness before saving.
 * email       — where the verification link is sent. Validated for uniqueness before saving.
 * displayName — shown in the UI (full name or alias).
 * tenantId    — which tenant this user belongs to.
 *               In most cases the admin's own tenantId from their JWT, but
 *               a SYSTEM_ADMIN can create users in any tenant.
 *
 * No password field — the admin never sets a password.
 * The user chooses their own password when they click the verification link.
 */
public record CreateUserRequest(
        String username,
        String email,
        String displayName,
        UUID   tenantId
) {}
