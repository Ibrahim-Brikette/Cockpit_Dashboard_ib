import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { Dashboard } from '@core/models/types';
import { SharingService, ShareGrantDto, CreateShareGrantRequest } from '@core/services/sharing.service';
import { IdentityService } from '@core/services/identity.service';
import { GroupService } from '@core/services/group.service';
import { AuditService } from '@pages/settings/services/audit.service';
import { DashboardService } from '@pages/dashboard/services/dashboard.service';
import { UserAccountDto } from '@core/api/dtos/user.dto';
import { SvgIconComponent } from '@shared/components/svg-icon/svg-icon.component';

interface AccessPerson {
  grantId:       string;   // Backend grant UUID; '' when isPending
  sourceId?:     string;   // Picker item UUID (user or group); set when isPending
  type:          'user' | 'group';
  name:          string;
  initials:      string;
  role:          'READ' | 'EDIT' | 'OWNER';
  pendingRemove: boolean;
  isPending?:    boolean;  // True = staged add, not yet saved to backend
}

interface PickerItem {
  id:       string;
  type:     'user' | 'group';
  name:     string;
  initials: string;
}

@Component({
  selector: 'app-share-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, SvgIconComponent],
  template: `
    <div *ngIf="open" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div class="w-full max-w-[520px] rounded-xl bg-white shadow-pop overflow-hidden">

        <!-- Header -->
        <div class="flex items-center justify-between border-b border-line px-5 py-4">
          <h2 class="text-sm font-bold text-ink">
            Partager — {{ dashboard?.name || 'Tableau de bord' }}
          </h2>
          <button type="button" (click)="onClose.emit()"
            class="rounded-md p-1 text-ink-faint hover:bg-surface-sunken hover:text-ink transition-colors">
            <app-svg-icon name="X" class="h-4 w-4"></app-svg-icon>
          </button>
        </div>

        <!-- Loading -->
        <div *ngIf="loading" class="flex items-center justify-center py-12">
          <div class="h-5 w-5 animate-spin rounded-full border-2 border-brand border-t-transparent"></div>
          <span class="ml-2 text-xs text-ink-faint">Chargement…</span>
        </div>

        <!-- Error -->
        <div *ngIf="error && !loading" class="mx-5 mt-4 rounded-lg border border-negative/30 bg-negative-soft px-3 py-2 text-xs text-negative">
          {{ error }}
        </div>

        <!-- Body -->
        <div *ngIf="!loading" class="p-5 space-y-5 text-xs max-h-[70vh] overflow-y-auto">

          <!-- NIVEAU DE PARTAGE -->
          <div>
            <div class="mb-2 text-[11px] font-semibold uppercase tracking-wider text-ink-faint">NIVEAU DE PARTAGE</div>
            <div class="space-y-2">
              <div *ngFor="let lvl of shareLevels"
                (click)="selectedLevel = lvl.value"
                class="flex items-center justify-between rounded-xl border p-3 cursor-pointer transition-all"
                [ngClass]="selectedLevel === lvl.value ? 'border-brand bg-blue-50/60 ring-1 ring-brand' : 'border-line hover:border-line-strong bg-white'">
                <div class="flex items-center gap-3">
                  <div class="text-ink-soft">
                    <app-svg-icon [name]="lvl.icon" class="h-4 w-4"></app-svg-icon>
                  </div>
                  <div>
                    <div class="font-bold text-ink text-xs">{{ lvl.label }}</div>
                    <div class="text-[11px] text-ink-faint">{{ lvl.hint }}</div>
                  </div>
                </div>
                <div class="h-4 w-4 rounded-full border flex items-center justify-center"
                  [ngClass]="selectedLevel === lvl.value ? 'border-brand bg-brand' : 'border-line-strong bg-white'">
                  <div *ngIf="selectedLevel === lvl.value" class="h-1.5 w-1.5 rounded-full bg-white"></div>
                </div>
              </div>
            </div>
          </div>

          <!-- PERSONNES AYANT ACCÈS -->
          <div *ngIf="selectedLevel !== 'private'">
            <div class="mb-2 text-[11px] font-semibold uppercase tracking-wider text-ink-faint">PERSONNES AYANT ACCÈS</div>

            <!-- grant list (saved + pending) -->
            <div *ngIf="activeList.length > 0"
              class="overflow-hidden rounded-xl border border-line bg-white divide-y divide-line mb-3">
              <div *ngFor="let p of activeList"
                class="flex items-center justify-between px-3 py-2.5"
                [ngClass]="p.pendingRemove ? 'opacity-40' : ''">
                <div class="flex items-center gap-2.5">
                  <div class="flex h-7 w-7 items-center justify-center rounded-full text-[11px] font-semibold"
                    [ngClass]="p.type === 'group' ? 'bg-purple-100 text-purple-700' : 'bg-slate-100 text-slate-700'">
                    {{ p.initials }}
                  </div>
                  <div>
                    <div class="flex items-center gap-1.5">
                      <span class="font-semibold text-ink text-xs">{{ p.name }}</span>
                      <span *ngIf="p.isPending"
                        class="rounded-full bg-green-100 px-1.5 py-0.5 text-[9px] font-bold uppercase tracking-wide text-green-700">
                        à ajouter
                      </span>
                    </div>
                    <span class="text-[10px] text-ink-faint">{{ p.type === 'group' ? 'Groupe' : 'Utilisateur' }}</span>
                  </div>
                </div>
                <div class="flex items-center gap-2">
                  <select *ngIf="!p.pendingRemove"
                    [(ngModel)]="p.role"
                    class="h-7 rounded-lg border border-line-strong bg-white px-2 text-xs font-medium text-ink outline-none focus:border-brand cursor-pointer">
                    <option value="READ">Lecture</option>
                    <option value="EDIT">Édition</option>
                    <option value="OWNER">Propriétaire</option>
                  </select>
                  <button type="button" (click)="toggleRemove(p)"
                    class="p-1 rounded transition-colors"
                    [ngClass]="p.pendingRemove ? 'text-brand hover:text-ink-soft' : 'text-ink-faint hover:text-negative'">
                    <app-svg-icon [name]="p.pendingRemove ? 'RotateCcw' : 'X'" class="h-3.5 w-3.5"></app-svg-icon>
                  </button>
                </div>
              </div>
            </div>

            <!-- empty state — only when no saved AND no pending grants -->
            <p *ngIf="activeList.length === 0" class="text-[11px] text-ink-faint italic mb-3">
              Aucun accès individuel configuré.
            </p>

            <!-- ADD PERSON -->
            <div class="rounded-xl border border-line bg-surface-muted/40 p-3 space-y-2">
              <div class="text-[11px] font-semibold text-ink-faint uppercase tracking-wider">Ajouter une personne ou un groupe</div>

              <!-- picker input -->
              <div class="relative">
                <input
                  [(ngModel)]="pickerSearch"
                  (input)="onPickerInput()"
                  (focus)="pickerOpen = true"
                  placeholder="Rechercher un utilisateur ou groupe…"
                  class="h-8 w-full rounded-lg border border-line-strong bg-white px-3 text-xs outline-none focus:border-brand focus:ring-1 focus:ring-brand"
                />
                <!-- results dropdown -->
                <div *ngIf="pickerOpen && pickerSearch && filteredPickerItems.length > 0"
                  class="absolute left-0 right-0 top-9 z-10 max-h-40 overflow-y-auto rounded-lg border border-line bg-white shadow-pop">
                  <button *ngFor="let item of filteredPickerItems" type="button"
                    (click)="selectPickerItem(item)"
                    class="flex w-full items-center gap-2 px-3 py-2 text-left text-xs hover:bg-surface-muted transition-colors">
                    <div class="flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[10px] font-semibold"
                      [ngClass]="item.type === 'group' ? 'bg-purple-100 text-purple-700' : 'bg-slate-100 text-slate-700'">
                      {{ item.initials }}
                    </div>
                    <div>
                      <span class="font-semibold text-ink">{{ item.name }}</span>
                      <span class="ml-1.5 text-[10px] text-ink-faint">{{ item.type === 'group' ? 'Groupe' : 'Utilisateur' }}</span>
                    </div>
                  </button>
                </div>
                <!-- no results -->
                <div *ngIf="pickerOpen && pickerSearch && filteredPickerItems.length === 0 && !loading"
                  class="absolute left-0 right-0 top-9 z-10 rounded-lg border border-line bg-white shadow-pop px-3 py-2.5 text-xs text-ink-faint italic">
                  Aucun résultat.
                </div>
              </div>

              <!-- selected item + role -->
              <div *ngIf="selectedPickerItem" class="flex items-center gap-2">
                <div class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-[11px] font-semibold"
                  [ngClass]="selectedPickerItem.type === 'group' ? 'bg-purple-100 text-purple-700' : 'bg-slate-100 text-slate-700'">
                  {{ selectedPickerItem.initials }}
                </div>
                <span class="flex-1 truncate text-xs font-semibold text-ink">{{ selectedPickerItem.name }}</span>
                <select [(ngModel)]="pendingRole"
                  class="h-7 rounded-lg border border-line-strong bg-white px-2 text-xs font-medium text-ink outline-none focus:border-brand cursor-pointer">
                  <option value="READ">Lecture</option>
                  <option value="EDIT">Édition</option>
                  <option value="OWNER">Propriétaire</option>
                </select>
                <button type="button" (click)="addPending()"
                  class="h-7 rounded-lg bg-brand px-3 text-xs font-semibold text-white hover:bg-brand-strong transition-colors">
                  Ajouter
                </button>
              </div>
            </div>

            <p class="mt-2 text-[11px] text-ink-faint">
              Les bénéficiaires reçoivent une notification avec un lien direct vers le tableau de bord.
            </p>
          </div>
        </div>

        <!-- Footer -->
        <div class="flex items-center justify-end gap-2 border-t border-line bg-surface-muted/30 px-5 py-3.5">
          <button type="button" (click)="onClose.emit()"
            class="rounded-lg border border-line-strong bg-white px-4 py-2 text-xs font-semibold text-ink hover:bg-surface-sunken transition-colors">
            Annuler
          </button>
          <button type="button" (click)="save()" [disabled]="saving || loading"
            class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-4 py-2 text-xs font-semibold text-white hover:bg-brand-strong shadow-2xs transition-colors disabled:opacity-60">
            <div *ngIf="saving" class="h-3.5 w-3.5 animate-spin rounded-full border-2 border-white border-t-transparent"></div>
            {{ saving ? 'Enregistrement…' : 'Enregistrer le partage' }}
          </button>
        </div>
      </div>
    </div>
  `
})
export class ShareModalComponent implements OnChanges {
  @Input() dashboard: Dashboard | null = null;
  @Input() open: boolean = false;
  @Output() onClose = new EventEmitter<void>();

  selectedLevel: 'private' | 'users' | 'group' | 'organization' = 'private';
  accessList: AccessPerson[] = [];
  loading = false;
  saving = false;
  error = '';

  // picker state
  pickerSearch = '';
  pickerOpen = false;
  pickerItems: PickerItem[] = [];
  filteredPickerItems: PickerItem[] = [];
  selectedPickerItem: PickerItem | null = null;
  pendingRole: 'READ' | 'EDIT' | 'OWNER' = 'READ';

  readonly shareLevels = [
    { value: 'private' as const,      label: 'Privé',                  hint: 'Vous uniquement.',                               icon: 'Lock'  },
    { value: 'users' as const,        label: 'Utilisateurs désignés',  hint: 'Personnes spécifiques.',                         icon: 'User'  },
    { value: 'group' as const,        label: 'Groupe / équipe',        hint: 'Tous les membres d\'un groupe.',                 icon: 'Users' },
    { value: 'organization' as const, label: 'Organisation',           hint: 'Tous les utilisateurs authentifiés.',            icon: 'Globe' },
  ];

  constructor(
    private sharingService: SharingService,
    private identityService: IdentityService,
    private groupService: GroupService,
    private auditService: AuditService,
    private dashboardService: DashboardService
  ) {}

  /** Entries that are either saved grants or staged-to-add (not pending removal). */
  get activeList(): AccessPerson[] {
    return this.accessList;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open'] && this.open && this.dashboard) {
      this.selectedLevel = (this.dashboard.shareLevel as any) || 'private';
      this.resetPicker();
      this.loadData();
    }
    if (changes['open'] && !this.open) {
      this.error = '';
      this.saving = false;
    }
  }

  private loadData(): void {
    this.loading = true;
    this.error = '';
    this.accessList = [];

    console.debug('[ShareModal] loadData start — dashboard:', this.dashboard?.id);

    forkJoin({
      grants: this.sharingService.getDashboardGrants(this.dashboard!.id).pipe(catchError(err => {
        console.debug('[ShareModal] getDashboardGrants error:', err.status, err.message);
        return of([]);
      })),
      users:  this.identityService.getAllUsers().pipe(catchError(err => {
        console.debug('[ShareModal] getAllUsers error:', err.status, err.message);
        return of([]);
      })),
      groups: this.groupService.getGroups().pipe(catchError(err => {
        console.debug('[ShareModal] getGroups error:', err.status, err.message);
        return of([]);
      }))
    }).pipe(
      finalize(() => { this.loading = false; })
    ).subscribe({
      next: ({ grants, users, groups }) => {
        console.debug('[ShareModal] loadData response — grants:', grants.length, 'users:', (users as any[]).length, 'groups:', (groups as any[]).length);
        this.buildAccessList(grants as any[]);
        this.buildPickerItems(users as any[], groups as any[]);
      },
      error: (err) => {
        console.debug('[ShareModal] loadData forkJoin error:', err);
        this.error = 'Impossible de charger les accès. Vérifiez vos permissions.';
      }
    });
  }

  private buildAccessList(grants: ShareGrantDto[]): void {
    this.accessList = grants.map(g => {
      const isGroup = !!g.granteeGroupId;
      const name = g.granteeName || (isGroup ? 'Groupe' : 'Utilisateur');
      return {
        grantId:       g.id,
        sourceId:      isGroup ? g.granteeGroupId : g.granteeUserId,
        type:          isGroup ? 'group' : 'user',
        name,
        initials:      this.buildInitials(name),
        role:          (g.accessLevel as 'READ' | 'EDIT' | 'OWNER') || 'READ',
        pendingRemove: false,
        isPending:     false
      };
    });
    console.debug('[ShareModal] accessList built:', this.accessList.length, 'entries');
  }

  private buildPickerItems(users: UserAccountDto[], groups: any[]): void {
    const userItems: PickerItem[] = users
      .filter(u => u.id)
      .map(u => ({
        id:       u.id!,
        type:     'user' as const,
        name:     u.displayName || u.username,
        initials: this.buildInitials(u.displayName || u.username)
      }));

    const groupItems: PickerItem[] = groups.map(g => ({
      id:       g.id,
      type:     'group' as const,
      name:     g.groupName,
      initials: this.buildInitials(g.groupName)
    }));

    this.pickerItems = [...userItems, ...groupItems];
    this.filteredPickerItems = [];
    console.debug('[ShareModal] pickerItems built:', userItems.length, 'users +', groupItems.length, 'groups');
  }

  onPickerInput(): void {
    const q = this.pickerSearch.toLowerCase().trim();
    if (!q) {
      this.filteredPickerItems = [];
      return;
    }
    // Exclude items already in accessList (saved or pending, not marked for removal)
    const activeSourceIds = new Set(
      this.accessList.filter(a => !a.pendingRemove && a.sourceId).map(a => a.sourceId!)
    );
    const activeNames = new Set(
      this.accessList.filter(a => !a.pendingRemove && !a.sourceId).map(a => a.name)
    );
    this.filteredPickerItems = this.pickerItems
      .filter(item =>
        item.name.toLowerCase().includes(q) &&
        !activeSourceIds.has(item.id) &&
        !activeNames.has(item.name)
      )
      .slice(0, 8);
    console.debug('[ShareModal] onPickerInput:', JSON.stringify(q), '→', this.filteredPickerItems.length, 'results');
  }

  selectPickerItem(item: PickerItem): void {
    console.debug('[ShareModal] selectPickerItem:', item);
    this.selectedPickerItem = item;
    this.pickerSearch = item.name;
    this.pickerOpen = false;
    this.filteredPickerItems = [];
  }

  addPending(): void {
    if (!this.selectedPickerItem) return;
    const item = this.selectedPickerItem;
    const entry: AccessPerson = {
      grantId:       '',
      sourceId:      item.id,
      type:          item.type,
      name:          item.name,
      initials:      item.initials,
      role:          this.pendingRole,
      pendingRemove: false,
      isPending:     true
    };
    // Reassign (not push) to guarantee Angular detects the change
    this.accessList = [...this.accessList, entry];
    console.debug('[ShareModal] addPending: accessList now', this.accessList.length, 'entries', this.accessList);
    this.resetPicker();
  }

  toggleRemove(p: AccessPerson): void {
    if (p.isPending) {
      // Staged add — just remove from the list entirely (never reached the backend)
      this.accessList = this.accessList.filter(a => a !== p);
    } else {
      p.pendingRemove = !p.pendingRemove;
    }
  }

  resetPicker(): void {
    this.pickerSearch = '';
    this.pickerOpen = false;
    this.selectedPickerItem = null;
    this.pendingRole = 'READ';
    this.filteredPickerItems = [];
  }

  roleLabel(role: string): string {
    return role === 'READ' ? 'Lecture' : role === 'EDIT' ? 'Édition' : 'Propriétaire';
  }

  save(): void {
    if (!this.dashboard) return;
    this.saving = true;
    this.error = '';

    const removes = this.accessList
      .filter(p => p.pendingRemove && !p.isPending)
      .map(p => this.sharingService.removeDashboardGrant(this.dashboard!.id, p.grantId));

    const adds = this.accessList
      .filter(p => p.isPending && !p.pendingRemove)
      .map(p => {
        const req: CreateShareGrantRequest = {
          shareLevel:     p.type === 'group' ? 'GROUP' : 'USERS',
          accessLevel:    p.role,
          granteeUserId:  p.type === 'user'  ? p.sourceId : undefined,
          granteeGroupId: p.type === 'group' ? p.sourceId : undefined
        };
        console.debug('[ShareModal] save → addDashboardGrant:', req);
        return this.sharingService.addDashboardGrant(this.dashboard!.id, req);
      });

    // Update shareLevel on the dashboard
    this.dashboard.shareLevel = this.selectedLevel as any;
    this.dashboardService.upsertDashboard(this.dashboard);

    const allOps = [...removes, ...adds];

    if (allOps.length === 0) {
      this.logAudit();
      this.saving = false;
      this.onClose.emit();
      return;
    }

    forkJoin(allOps).subscribe({
      next: () => {
        this.logAudit();
        this.saving = false;
        this.onClose.emit();
      },
      error: (err) => {
        console.debug('[ShareModal] save forkJoin error:', err);
        this.error = 'Erreur lors de la mise à jour des accès. Veuillez réessayer.';
        this.saving = false;
      }
    });
  }

  private logAudit(): void {
    if (!this.dashboard) return;
    const levelLabel = this.shareLevels.find(l => l.value === this.selectedLevel)?.label || this.selectedLevel;
    this.auditService.logAuditEvent(
      'Partage du tableau de bord',
      `${this.dashboard.name} · ${levelLabel}`,
      'DASHBOARD',
      this.dashboard.id
    );
  }

  private buildInitials(name: string): string {
    return (name || '?').split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  }
}
