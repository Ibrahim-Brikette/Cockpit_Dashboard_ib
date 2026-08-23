/**
 * Application roles — mirror of AppRole.java (shared/security/authorization/AppRole.java).
 *
 * When adding a new role:
 *   1. Add the constant here.
 *   2. Add it to AppRole.java on the backend.
 *   3. Add a DB migration to seed the role row and its permissions.
 *   4. Update any @PreAuthorize / hasRole() guards that need to include the new role.
 */
export enum AppRole {
  // ── System-wide ────────────────────────────────────────────────────────────
  /** Full system access. Max 2 accounts. Hardware MFA required. */
  SUPER_ADMIN        = 'SUPER_ADMIN',

  // ── Tenant-scoped ──────────────────────────────────────────────────────────
  /** Manages tenant users, datasources, all dashboards within tenant. MFA required. */
  TENANT_ADMIN       = 'TENANT_ADMIN',

  /** Create/edit/delete datasources and query templates. Cannot view credentials after save. */
  DATASOURCE_MANAGER = 'DATASOURCE_MANAGER',

  /** Create/edit/delete own dashboards and queries; share dashboards. Cannot create datasources. */
  DASHBOARD_CREATOR  = 'DASHBOARD_CREATOR',

  /** View shared dashboards only. Cannot edit. Cannot see queries or datasources. Default role. */
  DASHBOARD_VIEWER   = 'DASHBOARD_VIEWER',

  /** Add-on role: AI SQL generation; subject to rate limits. Requires a base role. */
  AI_USER            = 'AI_USER',

  /** Execute existing queries; view dashboards; no create/edit rights. */
  READONLY_ANALYST   = 'READONLY_ANALYST',
}

/** Roles that can manage other users — used for UI visibility guards. */
export const ADMIN_ROLES: AppRole[] = [AppRole.SUPER_ADMIN, AppRole.TENANT_ADMIN];
