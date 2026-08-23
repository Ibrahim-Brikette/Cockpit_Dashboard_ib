package com.dynamicdashboard.cockpit.shared.security.authorization;

/**
 * Canonical application roles — single source of truth for role names.
 *
 * Mirror of the frontend AppRole enum (core/enums/app-role.enum.ts).
 *
 * When adding a new role:
 *   1. Add the constant here.
 *   2. Add it to the frontend enum.
 *   3. Add a DB migration to seed the role row and its role_permission rows.
 *   4. Update @IsTenantAdmin / @PreAuthorize expressions if the new role
 *      should bypass existing permission checks.
 *
 * Spring Security prefixes role names with "ROLE_" internally.
 *   - Use name()        for @PreAuthorize hasRole() strings  → "SUPER_ADMIN"
 *   - Use springRole()  for programmatic authority checks     → "ROLE_SUPER_ADMIN"
 */
public enum AppRole {

    // ── System-wide ──────────────────────────────────────────────────────────

    /**
     * Full system access: user management, system configuration, all tenants.
     * Max 2 accounts. Hardware MFA required.
     */
    SUPER_ADMIN,

    // ── Tenant-scoped ────────────────────────────────────────────────────────

    /**
     * Manages tenant users, datasources, and all dashboards within the tenant.
     * MFA required.
     */
    TENANT_ADMIN,

    /**
     * Create / edit / delete datasources and query templates.
     * Cannot view datasource credentials in plaintext after save.
     */
    DATASOURCE_MANAGER,

    /**
     * Create / edit / delete own dashboards and queries; share dashboards.
     * Cannot create datasources.
     */
    DASHBOARD_CREATOR,

    /**
     * View shared dashboards only.
     * Cannot edit. Cannot see queries or datasources.
     * Default role for new users.
     */
    DASHBOARD_VIEWER,

    /**
     * Add-on role: use the AI SQL generation feature; subject to rate limits.
     * Must be combined with a base role (DASHBOARD_CREATOR, READONLY_ANALYST, etc.).
     */
    AI_USER,

    /**
     * Execute existing queries; view dashboards; no create / edit rights.
     * Data consumer role.
     */
    READONLY_ANALYST;

    /**
     * Returns the full Spring Security authority string.
     * Spring Security stores roles as "ROLE_" + name() internally.
     *
     * Use this in permission evaluators and any programmatic authority check
     * instead of hardcoding the string literal.
     *
     * Example: AppRole.SUPER_ADMIN.springRole() → "ROLE_SUPER_ADMIN"
     */
    public String springRole() {
        return "ROLE_" + this.name();
    }
}
