/**
 * Query permission codes — mirror of QueryPermission.java
 * (shared/security/authorization/QueryPermission.java).
 *
 * Values must match the permission_code rows seeded in the database.
 * When adding a new permission code, update both this file and the Java enum.
 */
export enum QueryPermission {
  VIEW       = 'query:view',
  CREATE     = 'query:create',
  EDIT       = 'query:edit',
  DELETE     = 'query:delete',
  EXECUTE    = 'query:execute',
  /** TENANT_ADMIN scope — manage all queries within the tenant. */
  MANAGE_ALL = 'query:manage_all',
}
