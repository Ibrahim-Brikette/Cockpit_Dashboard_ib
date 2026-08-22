/**
 * Datasource permission codes — mirror of DatasourcePermission.java
 * (shared/security/authorization/DatasourcePermission.java).
 *
 * Values must match the permission_code rows seeded in the database.
 * When adding a new permission code, update both this file and the Java enum.
 *
 * Note: datasource:delete and datasource:manage_all are commented out on the backend
 * pending spec clarification — kept commented here for consistency.
 */
export enum DatasourcePermission {
  /** Metadata only — credentials are never returned via this permission. */
  VIEW       = 'datasource:view',
  CREATE     = 'datasource:create',
  EDIT       = 'datasource:edit',
  MANAGE_ALL = 'datasource:manage-all',
  GET_FIELDS = 'datasource:get-fields',
  // DELETE  = 'datasource:delete',        // pending spec clarification
}
