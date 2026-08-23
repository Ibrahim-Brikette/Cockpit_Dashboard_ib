import { Injectable } from '@angular/core';
import { AuthService } from '@core/services/auth.service';
import { AppRole } from '@core/enums/app-role.enum';
import { Dashboard, DataQuery } from '@core/models/types';

/**
 * Mirrors RolePermissionSeeder.java exactly.
 * These are the permission codes granted to each role at startup (STANDALONE mode).
 * In INTEGRATED mode the backend still enforces the same rules — this service
 * only governs what the UI shows or hides.
 */
const ROLE_PERMISSIONS: Record<string, string[]> = {
  [AppRole.SUPER_ADMIN]: [
    'dashboard:manage_all',
    'query:manage_all',
    'datasource:manage-all'
  ],
  [AppRole.TENANT_ADMIN]: [
    'dashboard:manage_all',
    'query:manage_all',
    'datasource:view'
  ],
  [AppRole.DATASOURCE_MANAGER]: [
    'datasource:create',
    'datasource:edit',
    'datasource:view',
    'datasource:get-fields',
    'query:create',
    'query:edit'
  ],
  [AppRole.DASHBOARD_CREATOR]: [
    'dashboard:create',
    'dashboard:edit',
    'dashboard:delete',
    'dashboard:share',
    'query:create',
    'query:edit',
    'query:delete',
    'query:execute'
  ],
  [AppRole.DASHBOARD_VIEWER]: [
    'dashboard:view'
  ],
  [AppRole.READONLY_ANALYST]: [
    'query:execute',
    'dashboard:view'
  ],
  [AppRole.AI_USER]: []
};

@Injectable({ providedIn: 'root' })
export class PermissionService {

  constructor(private authService: AuthService) {}

  /**
   * Returns true if any of the current user's roles grants the given permission code.
   * manage_all permissions also cover all narrower operations on the same resource type.
   */
  hasPermission(code: string): boolean {
    const user = this.authService.currentUser;
    if (!user) return false;

    // Check manage_all shortcut (e.g. dashboard:manage_all covers dashboard:create etc.)
    const [resource] = code.split(':');
    const manageAll = `${resource}:manage_all`;
    const manageAllAlt = `${resource}:manage-all`; // datasource uses a dash

    for (const role of Object.keys(ROLE_PERMISSIONS)) {
      if (!this.authService.hasRole(role)) continue;
      const perms = ROLE_PERMISSIONS[role];
      if (perms.includes(code)) return true;
      if (perms.includes(manageAll)) return true;
      if (perms.includes(manageAllAlt)) return true;
    }
    return false;
  }

  // ── Dashboard permissions ──────────────────────────────────────────────────

  /** Can create a new dashboard. */
  canCreateDashboard(): boolean {
    return this.hasPermission('dashboard:create');
  }

  /**
   * Can view a dashboard — owns it OR has dashboard:view (or wider).
   * Backend mirrors this: @PreAuthorize("hasPermission(#id, 'DASHBOARD', 'VIEW')")
   */
  canViewDashboard(d: Dashboard): boolean {
    return this.hasPermission('dashboard:view') || this.isOwner(d);
  }

  /**
   * Can edit/modify a dashboard — owns it OR has dashboard:edit (or manage_all).
   */
  canManageDashboard(d: Dashboard): boolean {
    if (this.hasPermission('dashboard:manage_all')) return true;
    if (!this.hasPermission('dashboard:edit')) return false;
    return this.isOwner(d);
  }

  /**
   * Can share a dashboard — owns it OR has dashboard:share (or manage_all).
   */
  canShareDashboard(d: Dashboard): boolean {
    if (this.hasPermission('dashboard:manage_all')) return true;
    if (!this.hasPermission('dashboard:share')) return false;
    return this.isOwner(d);
  }

  /**
   * Can delete a dashboard — owns it OR has dashboard:delete (or manage_all).
   */
  canDeleteDashboard(d: Dashboard): boolean {
    if (this.hasPermission('dashboard:manage_all')) return true;
    if (!this.hasPermission('dashboard:delete')) return false;
    return this.isOwner(d);
  }

  // ── Query permissions ──────────────────────────────────────────────────────

  canCreateQuery(): boolean  { return this.hasPermission('query:create'); }
  canEditQuery(): boolean    { return this.hasPermission('query:edit'); }
  canDeleteQuery(): boolean  { return this.hasPermission('query:delete'); }
  canExecuteQuery(): boolean { return this.hasPermission('query:execute'); }

  /**
   * Can share a query — owns it OR has query:share (or manage_all).
   * Mirrors SharingController: hasPermission(#id, 'Query', 'query:share')
   */
  canShareQuery(q: DataQuery): boolean {
    if (this.hasPermission('query:manage_all')) return true;
    if (!this.hasPermission('query:share')) return false;
    return this.isQueryOwner(q);
  }

  // ── Datasource permissions ─────────────────────────────────────────────────

  canViewDatasource(): boolean   { return this.hasPermission('datasource:view'); }
  canCreateDatasource(): boolean { return this.hasPermission('datasource:create'); }
  canEditDatasource(): boolean   { return this.hasPermission('datasource:edit'); }

  // ── Admin / sharing ────────────────────────────────────────────────────────

  /** Can access the Users admin page and create/delete users. */
  canManageUsers(): boolean {
    return this.authService.hasRole(AppRole.TENANT_ADMIN) ||
           this.authService.hasRole(AppRole.SUPER_ADMIN);
  }

  /** Can manage dashboard share grants (the sharing modal). Mirrors SharingController. */
  canManageShares(d: Dashboard): boolean {
    return this.canShareDashboard(d);
  }

  /** Can manage query share grants. Mirrors SharingController. */
  canManageQueryShares(q: DataQuery): boolean {
    return this.canShareQuery(q);
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  private isOwner(d: Dashboard): boolean {
    const user = this.authService.currentUser;
    if (!user) return false;
    return !!d.ownerId && d.ownerId === user.id;
  }

  private isQueryOwner(q: DataQuery): boolean {
    const user = this.authService.currentUser;
    if (!user) return false;
    return !!q.ownerId && q.ownerId === user.id;
  }
}
