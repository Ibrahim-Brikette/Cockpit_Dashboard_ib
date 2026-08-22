import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, finalize } from 'rxjs';
import { IdentityService } from '@core/services/identity.service';
import { AuthService } from '@core/services/auth.service';
import { RoleService } from '@core/services/role.service';
import { GroupService } from '@core/services/group.service';
import { AppRole } from '@core/enums/app-role.enum';
import {
  UserAccountDto,
  RoleDto, UserRoleDto,
  GroupDto, UserGroupDto, GroupMemberDto
} from '@core/api/dtos/user.dto';
import { SvgIconComponent } from '@shared/components/svg-icon/svg-icon.component';

type DetailTab = 'profil' | 'roles' | 'groupes';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, SvgIconComponent],
  templateUrl: './users.component.html'
})
export class UsersComponent implements OnInit {

  // ── List ──────────────────────────────────────────────────────────────────
  users: UserAccountDto[] = [];
  loadingUsers = false;
  errorUsers: string | null = null;

  // ── Create user panel ─────────────────────────────────────────────────────
  panelOpen = false;
  submitting = false;
  createError: string | null = null;
  createSuccess = false;
  form: FormGroup;

  // ── Create group panel ────────────────────────────────────────────────────
  groupPanelOpen    = false;
  groupSubmitting   = false;
  groupCreateError: string | null = null;
  groupCreateSuccess = false;
  groupForm: FormGroup;

  // ── Detail panel ──────────────────────────────────────────────────────────
  selectedUser: UserAccountDto | null = null;
  detailTab: DetailTab = 'profil';

  // Roles tab
  allRoles: RoleDto[] = [];
  userRoles: UserRoleDto[] = [];
  checkedRoleIds = new Set<string>();
  originalRoleIds = new Set<string>();
  rolesLoading = false;
  rolesSaving = false;
  rolesError: string | null = null;
  rolesSaved = false;

  // Groups tab
  allGroups: GroupDto[] = [];
  userGroups: UserGroupDto[] = [];
  groupsLoading = false;
  groupsError: string | null = null;
  addGroupId = '';
  addMembershipRole = 'MEMBER';
  addingGroup = false;

  // ── Page tab ───────────────────────────────────────────────────────────────
  pageTab: 'utilisateurs' | 'groupes' = 'utilisateurs';

  // ── Groups page list ───────────────────────────────────────────────────────
  groups: GroupDto[] = [];
  loadingGroups = false;
  errorGroups: string | null = null;

  // ── Group detail panel ─────────────────────────────────────────────────────
  selectedGroup: GroupDto | null = null;
  groupMembers: GroupMemberDto[] = [];
  loadingGroupMembers = false;
  errorGroupMembers: string | null = null;
  addMemberUserId = '';
  addMemberRole = 'MEMBER';
  addingMember = false;

  // ── Delete group ───────────────────────────────────────────────────────────
  deleteGroupTarget: GroupDto | null = null;
  deleteGroupMemberCount = 0;
  deletingGroup = false;
  deleteGroupError: string | null = null;

  // ── Delete user
  deleteTarget: UserAccountDto | null = null;
  deleting = false;
  deleteError: string | null = null;

  constructor(
    private identityService: IdentityService,
    private authService:     AuthService,
    private roleService:     RoleService,
    private groupService:    GroupService,
    private fb:              FormBuilder,
    private cdr:             ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      displayName: ['', [Validators.required, Validators.maxLength(120)]],
      username:    ['', [Validators.required, Validators.maxLength(80)]],
      email:       ['', [Validators.required, Validators.email, Validators.maxLength(180)]]
    });

    this.groupForm = this.fb.group({
      groupName:        ['', [Validators.required, Validators.maxLength(120)]],
      groupCode:        ['', [Validators.required, Validators.maxLength(80)]],
      groupDescription: ['', Validators.maxLength(255)]
    });
  }

  ngOnInit(): void {
    this.loadUsers();
  }

  // isAdmin: was 'SYSTEM_ADMIN' — renamed to SUPER_ADMIN in the AppRole enum
  get isAdmin(): boolean {
    return this.authService.hasRole(AppRole.TENANT_ADMIN) || this.authService.hasRole(AppRole.SUPER_ADMIN);
  }

  get isStandalone(): boolean {
    return this.authService.currentMode === 'STANDALONE';
  }

  // ── List ──────────────────────────────────────────────────────────────────

  loadUsers(): void {
    this.loadingUsers = true;
    this.errorUsers   = null;
    this.identityService.getAllUsers().pipe(
      finalize(() => { this.loadingUsers = false; this.cdr.markForCheck(); })
    ).subscribe({
      next:  users => { this.users = users; this.cdr.markForCheck(); },
      error: ()    => { this.errorUsers = 'Impossible de charger les utilisateurs.'; }
    });
  }

  // ── Create panel ──────────────────────────────────────────────────────────

  openPanel(): void {
    this.form.reset();
    this.createError   = null;
    this.createSuccess = false;
    this.panelOpen     = true;
  }

  closePanel(): void {
    this.panelOpen = false;
  }

  fieldError(name: string, error: string): boolean {
    const c = this.form.controls[name];
    return !!(c.invalid && (c.dirty || c.touched) && c.errors?.[error]);
  }

  onSubmit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.submitting  = true;
    this.createError = null;
    const { displayName, username, email } = this.form.value;

    this.identityService.createUser(username, email, displayName).pipe(
      finalize(() => { this.submitting = false; this.cdr.markForCheck(); })
    ).subscribe({
      next: created => {
        this.users = [...this.users, created];
        this.createSuccess = true;
        this.form.reset();
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.createError = err.status === 400
          ? 'Nom d\'utilisateur ou email déjà utilisé.'
          : 'Une erreur est survenue. Veuillez réessayer.';
      }
    });
  }

  // ── Create group panel ────────────────────────────────────────────────────

  openGroupPanel(): void {
    this.groupForm.reset();
    this.groupCreateError   = null;
    this.groupCreateSuccess = false;
    this.groupPanelOpen     = true;
  }

  closeGroupPanel(): void {
    this.groupPanelOpen = false;
  }

  groupFieldError(name: string, error: string): boolean {
    const c = this.groupForm.controls[name];
    return !!(c.invalid && (c.dirty || c.touched) && c.errors?.[error]);
  }

  onGroupSubmit(): void {
    this.groupForm.markAllAsTouched();
    if (this.groupForm.invalid) return;

    this.groupSubmitting  = true;
    this.groupCreateError = null;
    const { groupName, groupCode, groupDescription } = this.groupForm.value;

    this.groupService.createGroup({ groupName, groupCode, groupDescription }).pipe(
      finalize(() => { this.groupSubmitting = false; this.cdr.markForCheck(); })
    ).subscribe({
      next: created => {
        this.allGroups = [created, ...this.allGroups];
        this.groups    = [created, ...this.groups];
        this.groupCreateSuccess = true;
        this.groupForm.reset();
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.groupCreateError = err.status === 400
          ? 'Ce code de groupe est déjà utilisé.'
          : 'Une erreur est survenue. Veuillez réessayer.';
      }
    });
  }

  // ── Detail panel ──────────────────────────────────────────────────────────

  selectUser(user: UserAccountDto): void {
    this.selectedUser = user;
    this.detailTab = 'profil';
    this.clearDetailState();
  }

  closeDetail(): void {
    this.selectedUser = null;
  }

  switchTab(tab: DetailTab): void {
    this.detailTab = tab;
    if (tab === 'roles')   this.loadRolesTab();
    if (tab === 'groupes') this.loadGroupesTab();
  }

  private clearDetailState(): void {
    this.allRoles = []; this.userRoles = [];
    this.checkedRoleIds = new Set(); this.originalRoleIds = new Set();
    this.rolesError = null; this.rolesSaved = false;
    this.userGroups = []; this.allGroups = [];
    this.groupsError = null; this.addGroupId = '';
  }

  // ── Roles tab ─────────────────────────────────────────────────────────────

  loadRolesTab(): void {
    if (!this.selectedUser?.id) return;
    this.rolesLoading = true;
    this.rolesError   = null;
    this.rolesSaved   = false;

    forkJoin({
      all:  this.roleService.getRoles(),
      mine: this.roleService.getUserRoles(this.selectedUser.id)
    }).pipe(
      finalize(() => { this.rolesLoading = false; this.cdr.markForCheck(); })
    ).subscribe({
      next: ({ all, mine }) => {
        this.allRoles  = all;
        this.userRoles = mine;
        const assigned = new Set(mine.map(r => r.roleId));
        this.checkedRoleIds  = new Set(assigned);
        this.originalRoleIds = new Set(assigned);
        this.cdr.markForCheck();
      },
      error: () => { this.rolesError = 'Impossible de charger les rôles.'; }
    });
  }

  isRoleChecked(roleId: string): boolean {
    return this.checkedRoleIds.has(roleId);
  }

  toggleRole(roleId: string): void {
    if (this.checkedRoleIds.has(roleId)) this.checkedRoleIds.delete(roleId);
    else                                  this.checkedRoleIds.add(roleId);
    this.rolesSaved = false;
  }

  get rolesDirty(): boolean {
    if (this.checkedRoleIds.size !== this.originalRoleIds.size) return true;
    for (const id of this.checkedRoleIds) {
      if (!this.originalRoleIds.has(id)) return true;
    }
    return false;
  }

  saveRoles(): void {
    if (!this.selectedUser?.id) return;
    const userId = this.selectedUser.id;

    const toAdd    = [...this.checkedRoleIds].filter(id => !this.originalRoleIds.has(id));
    const toRemove = [...this.originalRoleIds].filter(id => !this.checkedRoleIds.has(id));

    if (toAdd.length === 0 && toRemove.length === 0) {
      this.rolesSaved = true; this.cdr.markForCheck(); return;
    }

    this.rolesSaving = true;
    this.rolesError  = null;

    const ops$ = [
      ...toAdd.map(roleId    => this.roleService.assignRole(userId, roleId)),
      ...toRemove.map(roleId => this.roleService.removeRole(userId, roleId))
    ];

    forkJoin(ops$).pipe(
      finalize(() => { this.rolesSaving = false; this.cdr.markForCheck(); })
    ).subscribe({
      next:  () => { this.originalRoleIds = new Set(this.checkedRoleIds); this.rolesSaved = true; this.cdr.markForCheck(); },
      error: () => { this.rolesError = 'Erreur lors de la sauvegarde des rôles.'; }
    });
  }

  roleLabel(roleName: string): string {
    const labels: Record<string, string> = {
      SUPER_ADMIN:          'Super Admin',
      TENANT_ADMIN:         'Administrateur Tenant',
      DATASOURCE_MANAGER:   'Gestionnaire Sources',
      DASHBOARD_CREATOR:    'Créateur Dashboards',
      DASHBOARD_VIEWER:     'Lecteur Dashboards',
      AI_USER:              'Utilisateur IA',
      READONLY_ANALYST:     'Analyste Lecture Seule'
    };
    return labels[roleName] ?? roleName;
  }

  roleDesc(roleName: string): string {
    const descs: Record<string, string> = {
      SUPER_ADMIN:          'Accès total à toutes les fonctionnalités',
      TENANT_ADMIN:         'Gestion des utilisateurs et configurations du tenant',
      DATASOURCE_MANAGER:   'Création et gestion des sources de données',
      DASHBOARD_CREATOR:    'Création, édition et partage de dashboards',
      DASHBOARD_VIEWER:     'Lecture seule sur les dashboards partagés',
      AI_USER:              'Accès aux fonctionnalités d\'intelligence artificielle',
      READONLY_ANALYST:     'Exécution de requêtes, lecture des dashboards'
    };
    return descs[roleName] ?? '';
  }

  // ── Groups tab ────────────────────────────────────────────────────────────

  loadGroupesTab(): void {
    if (!this.selectedUser?.id) return;
    this.groupsLoading = true;
    this.groupsError   = null;

    forkJoin({
      myGroups: this.groupService.getUserGroups(this.selectedUser.id),
      all:      this.groupService.getGroups()
    }).pipe(
      finalize(() => { this.groupsLoading = false; this.cdr.markForCheck(); })
    ).subscribe({
      next: ({ myGroups, all }) => {
        this.userGroups = myGroups;
        this.allGroups  = all;
        this.cdr.markForCheck();
      },
      error: () => { this.groupsError = 'Impossible de charger les groupes.'; }
    });
  }

  availableGroups(): GroupDto[] {
    const memberIds = new Set(this.userGroups.map(g => g.groupId));
    return this.allGroups.filter(g => !memberIds.has(g.id));
  }

  addToGroup(): void {
    if (!this.selectedUser?.id || !this.addGroupId) return;
    this.addingGroup = true;

    this.groupService.addToGroup(this.addGroupId, this.selectedUser.id, this.addMembershipRole).pipe(
      finalize(() => { this.addingGroup = false; this.cdr.markForCheck(); })
    ).subscribe({
      next:  () => { this.addGroupId = ''; this.loadGroupesTab(); },
      error: () => { this.groupsError = 'Erreur lors de l\'ajout au groupe.'; }
    });
  }

  removeFromGroup(groupId: string): void {
    if (!this.selectedUser?.id) return;
    this.groupService.removeFromGroup(groupId, this.selectedUser.id).pipe(
      finalize(() => this.cdr.markForCheck())
    ).subscribe({
      next:  () => { this.userGroups = this.userGroups.filter(g => g.groupId !== groupId); this.cdr.markForCheck(); },
      error: () => { this.groupsError = 'Erreur lors de la suppression.'; }
    });
  }

  // ── Page tab switching ────────────────────────────────────────────────────

  switchPageTab(tab: 'utilisateurs' | 'groupes'): void {
    this.pageTab = tab;
    if (tab === 'groupes' && this.groups.length === 0 && !this.loadingGroups) {
      this.loadGroups();
    }
  }

  // ── Groups page list ──────────────────────────────────────────────────────

  loadGroups(): void {
    this.loadingGroups = true;
    this.errorGroups   = null;
    this.groupService.getGroups().pipe(
      finalize(() => { this.loadingGroups = false; this.cdr.markForCheck(); })
    ).subscribe({
      next:  groups => { this.groups = groups; this.cdr.markForCheck(); },
      error: ()     => { this.errorGroups = 'Impossible de charger les groupes.'; }
    });
  }

  // ── Group detail panel ────────────────────────────────────────────────────

  selectGroup(group: GroupDto): void {
    this.selectedGroup      = group;
    this.errorGroupMembers  = null;
    this.addMemberUserId    = '';
    this.loadGroupMembers(group.id);
  }

  closeGroupDetail(): void {
    this.selectedGroup = null;
  }

  loadGroupMembers(groupId: string): void {
    this.loadingGroupMembers = true;
    this.errorGroupMembers   = null;
    this.groupService.getGroupMembers(groupId).pipe(
      finalize(() => { this.loadingGroupMembers = false; this.cdr.markForCheck(); })
    ).subscribe({
      next:  members => { this.groupMembers = members; this.cdr.markForCheck(); },
      error: ()      => { this.errorGroupMembers = 'Impossible de charger les membres.'; }
    });
  }

  removeGroupMember(userId: string): void {
    if (!this.selectedGroup) return;
    const groupId = this.selectedGroup.id;
    this.groupService.removeFromGroup(groupId, userId).pipe(
      finalize(() => this.cdr.markForCheck())
    ).subscribe({
      next:  () => { this.groupMembers = this.groupMembers.filter(m => m.userId !== userId); this.cdr.markForCheck(); },
      error: () => { this.errorGroupMembers = 'Erreur lors de la suppression.'; }
    });
  }

  addMemberToGroup(): void {
    if (!this.selectedGroup || !this.addMemberUserId) return;
    const groupId = this.selectedGroup.id;
    this.addingMember = true;
    this.groupService.addToGroup(groupId, this.addMemberUserId, this.addMemberRole).pipe(
      finalize(() => { this.addingMember = false; this.cdr.markForCheck(); })
    ).subscribe({
      next:  () => { this.addMemberUserId = ''; this.loadGroupMembers(groupId); },
      error: () => { this.errorGroupMembers = 'Erreur lors de l\'ajout.'; }
    });
  }

  availableUsersForGroup(): UserAccountDto[] {
    const memberIds = new Set(this.groupMembers.map(m => m.userId));
    return this.users.filter(u => u.id && !memberIds.has(u.id));
  }

  groupInitials(group: GroupDto): string {
    return group.groupName.trim().split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  }

  memberInitials(member: GroupMemberDto): string {
    const name = member.displayName || member.username;
    return name.trim().split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  }

  confirmDeleteGroup(group: GroupDto, event: Event): void {
    event.stopPropagation();
    this.deleteGroupTarget      = group;
    this.deleteGroupMemberCount = this.selectedGroup?.id === group.id ? this.groupMembers.length : 0;
    this.deleteGroupError       = null;
  }

  cancelDeleteGroup(): void {
    this.deleteGroupTarget = null;
    this.deleteGroupError  = null;
  }

  executeDeleteGroup(): void {
    if (!this.deleteGroupTarget?.id) return;
    this.deletingGroup    = true;
    this.deleteGroupError = null;
    const targetId        = this.deleteGroupTarget.id;

    this.groupService.deleteGroup(targetId).pipe(
      finalize(() => { this.deletingGroup = false; this.cdr.markForCheck(); })
    ).subscribe({
      next: () => {
        this.groups = this.groups.filter(g => g.id !== targetId);
        if (this.selectedGroup?.id === targetId) this.selectedGroup = null;
        this.deleteGroupTarget = null;
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        console.error('[deleteGroup] failed', err);
        if (err.status === 403 || err.status === 401) {
          this.deleteGroupError = 'Permission refusée. Seuls les administrateurs peuvent supprimer un groupe.';
        } else if (err.status === 404) {
          this.deleteGroupError = 'Ce groupe n\'existe plus. Actualisez la liste.';
        } else if (err.status === 0) {
          this.deleteGroupError = 'Impossible de joindre le serveur. Vérifiez votre connexion.';
        } else {
          this.deleteGroupError = `Erreur inattendue (${err.status}). Réessayez ou contactez le support.`;
        }
      }
    });
  }

  membershipRoleLabel(role: string): string {
    const l: Record<string, string> = {
      OWNER: 'Propriétaire', ADMIN: 'Administrateur',
      MEMBER: 'Membre',      READER: 'Lecteur'
    };
    return l[role] ?? role;
  }

  // ── Delete user ───────────────────────────────────────────────────────────

  confirmDelete(user: UserAccountDto, event: Event): void {
    event.stopPropagation(); // prevent row click from opening the detail panel
    this.deleteTarget = user;
    this.deleteError  = null;
  }

  cancelDelete(): void {
    this.deleteTarget = null;
    this.deleteError  = null;
  }

  executeDelete(): void {
    if (!this.deleteTarget?.id) return;
    this.deleting    = true;
    this.deleteError = null;
    const targetId   = this.deleteTarget.id;

    this.identityService.deleteUser(targetId).pipe(
      finalize(() => { this.deleting = false; this.cdr.markForCheck(); })
    ).subscribe({
      next: () => {
        this.users = this.users.filter(u => u.id !== targetId);
        if (this.selectedUser?.id === targetId) this.selectedUser = null;
        this.deleteTarget = null;
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 400) {
          this.deleteError = 'Impossible de supprimer votre propre compte.';
        } else if (err.status === 409 && err.error?.message) {
          this.deleteError = err.error.message;
        } else if (err.status === 409) {
          this.deleteError = 'Impossible de supprimer l\'utilisateur : des données liées existent encore.';
        } else {
          this.deleteError = 'Une erreur est survenue. Veuillez réessayer.';
        }
      }
    });
  }

  // ── Shared helpers ────────────────────────────────────────────────────────

  statusLabel(status: string | undefined): string {
    switch (status) {
      case 'ACTIVE':   return 'Actif';
      case 'PENDING':  return 'En attente';
      case 'LOCKED':   return 'Verrouillé';
      case 'DISABLED': return 'Désactivé';
      default:         return status ? 'Inconnu' : '—';
    }
  }

  statusClass(status: string | undefined): string {
    switch (status) {
      case 'ACTIVE':   return 'bg-positive/10 text-positive';
      case 'PENDING':  return 'bg-caution/10 text-caution';
      case 'LOCKED':   return 'bg-negative/10 text-negative';
      case 'DISABLED': return 'bg-zinc-100 text-ink-faint dark:bg-zinc-800 dark:text-zinc-400';
      default:         return 'bg-zinc-100 text-ink-faint dark:bg-zinc-800 dark:text-zinc-400';
    }
  }

  initials(user: UserAccountDto): string {
    const name = user.displayName || user.username;
    return name.trim().split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  }

  formatDate(iso: string | undefined): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('fr-FR', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }
}
