/**
 * Dashboard permission codes — mirror of DashboardPermission.java
 * (shared/security/authorization/DashboardPermission.java).
 *
 * Values must match the permission_code rows seeded in the database.
 * When adding a new permission code, update both this file and the Java enum.
 */
export enum DashboardPermission {
  VIEW       = 'dashboard:view',
  CREATE     = 'dashboard:create',
  EDIT       = 'dashboard:edit',
  DELETE     = 'dashboard:delete',
  SHARE      = 'dashboard:share',
  /** TENANT_ADMIN scope — manage other users' dashboards within the tenant. */
  MANAGE_ALL = 'dashboard:manage_all',
}
