import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, of, Subject, throwError, TimeoutError } from 'rxjs';
import { catchError, finalize, takeUntil, timeout } from 'rxjs/operators';
import { DataQuery } from '@core/models/types';
import { SharingService, ShareGrantDto, CreateShareGrantRequest } from '@core/services/sharing.service';
import { IdentityService } from '@core/services/identity.service';
import { GroupService } from '@core/services/group.service';
import { AuditService } from '@pages/settings/services/audit.service';
import { UserAccountDto, GroupDto } from '@core/api/dtos/user.dto';
import { SvgIconComponent } from '@shared/components/svg-icon/svg-icon.component';

interface AccessPerson {
  grantId:       string;    // Backend UUID; '' when isPending
  sourceId?:     string;    // Picker item UUID; used in POST body when isPending
  type:          'user' | 'group';
  name:          string;
  initials:      string;
  role:          'READ' | 'EDIT' | 'OWNER';
  pendingRemove: boolean;
  isPending?:    boolean;
}

interface PickerItem {
  id: string;
  type: 'user' | 'group';
  name: string;
  initials: string;
}

@Component({
  selector: 'app-query-share-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, SvgIconComponent],
  template: `
    <div *ngIf="open" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div class="w-full max-w-[520px] rounded-xl bg-white shadow-pop overflow-hidden">

        <!-- Header -->
        <div class="flex items-center justify-between border-b border-line px-5 py-4">
          <h2 class="text-sm font-bold text-ink">
            Partager la requête — {{ query?.name || 'Requête' }}
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
        <div *ngIf="!loading && error"
          class="mx-5 mt-4 mb-2 rounded-lg border border-negative/30 bg-negative-soft px-3 py-2.5 text-xs text-negative flex items-center justify-between gap-3">
          <span>{{ error }}</span>
          <button type="button" (click)="retry()"
            class="shrink-0 rounded-md border border-negative/40 px-2.5 py-1 text-[11px] font-semibold hover:bg-negative/10 transition-colors">
            Réessayer
          </button>
        </div>

        <!-- Body -->
        <div *ngIf="!loading && !error" class="p-5 space-y-4 text-xs max-h-[70vh] overflow-y-auto">

          <!-- Existing grants -->
          <div>
            <div class="mb-2 text-[11px] font-semibold uppercase tracking-wider text-ink-faint">
              PERSONNES AYANT ACCÈS
            </div>

            <div *ngIf="accessList.length > 0"
              class="overflow-hidden rounded-xl border border-line bg-white divide-y divide-line mb-3">
              <div *ngFor="let p of accessList"
                class="flex items-center justify-between px-3 py-2.5"
                [ngClass]="p.pendingRemove ? 'opacity-40' : ''">
                <div class="flex items-center gap-2.5">
                  <div class="flex h-7 w-7 items-center justify-center rounded-full text-[11px] font-semibold"
                    [ngClass]="p.type === 'group' ? 'bg-purple-100 text-purple-700' : 'bg-slate-100 text-slate-700'">
                    {{ p.initials }}
                  </div>
                  <div>
                    <span class="font-semibold text-ink text-xs">{{ p.name }}</span>
                    <span class="ml-1.5 text-[10px] text-ink-faint">
                      {{ p.type === 'group' ? 'Groupe' : 'Utilisateur' }}
                    </span>
                  </div>
                </div>
                <div class="flex items-center gap-2">
                  <select *ngIf="!p.pendingRemove" [(ngModel)]="p.role"
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

            <p *ngIf="accessList.length === 0" class="text-[11px] text-ink-faint italic mb-3">
              Aucun accès individuel configuré.
            </p>
          </div>

          <!-- Add person -->
          <div class="rounded-xl border border-line bg-surface-muted/40 p-3 space-y-2">
            <div class="text-[11px] font-semibold text-ink-faint uppercase tracking-wider">
              Ajouter une personne ou un groupe
            </div>

            <!-- search input -->
            <div class="relative">
              <input
                [(ngModel)]="pickerSearch"
                (input)="onPickerInput()"
                (focus)="pickerOpen = true"
                placeholder="Rechercher par nom…"
                class="h-8 w-full rounded-lg border border-line-strong bg-white px-3 text-xs outline-none focus:border-brand focus:ring-1 focus:ring-brand"
              />
              <div *ngIf="pickerOpen && filteredPickerItems.length > 0"
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
                    <span class="ml-1.5 text-[10px] text-ink-faint">
                      {{ item.type === 'group' ? 'Groupe' : 'Utilisateur' }}
                    </span>
                  </div>
                </button>
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

            <!-- pending adds are shown inline in the accessList above with a green badge -->
          </div>

          <p class="text-[11px] text-ink-faint">
            Les accès s'appliquent à cette requête uniquement. Les widgets utilisant cette requête ne sont pas affectés.
          </p>
        </div>

        <!-- Footer -->
        <div class="flex items-center justify-end gap-2 border-t border-line bg-surface-muted/30 px-5 py-3.5">
          <button type="button" (click)="onClose.emit()"
            class="rounded-lg border border-line-strong bg-white px-4 py-2 text-xs font-semibold text-ink hover:bg-surface-sunken transition-colors">
            Annuler
          </button>
          <button type="button" (click)="save()" [disabled]="saving || loading || !!error"
            class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-4 py-2 text-xs font-semibold text-white hover:bg-brand-strong shadow-2xs transition-colors disabled:opacity-60">
            <div *ngIf="saving" class="h-3.5 w-3.5 animate-spin rounded-full border-2 border-white border-t-transparent"></div>
            {{ saving ? 'Enregistrement…' : 'Enregistrer' }}
          </button>
        </div>
      </div>
    </div>
  `
})
export class QueryShareModalComponent implements OnChanges, OnDestroy {
  @Input() query: DataQuery | null = null;
  @Input() open: boolean = false;
  @Output() onClose = new EventEmitter<void>();

  private destroy$ = new Subject<void>();
  private loadTimer?: number;

  accessList: AccessPerson[] = [];
  loading = false;
  saving = false;
  error = '';

  pickerSearch = '';
  pickerOpen = false;
  pickerItems: PickerItem[] = [];
  filteredPickerItems: PickerItem[] = [];
  selectedPickerItem: PickerItem | null = null;
  pendingRole: 'READ' | 'EDIT' | 'OWNER' = 'READ';

  constructor(
    private sharingService: SharingService,
    private identityService: IdentityService,
    private groupService: GroupService,
    private auditService: AuditService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open'] && !this.open) {
      this.destroy$.next();
      window.clearTimeout(this.loadTimer);
      this.loading = false;
      this.error   = '';
      this.saving  = false;
    }
    if (changes['open'] && this.open && this.query) {
      this.resetPicker();
      this.loadData();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    window.clearTimeout(this.loadTimer);
  }

  private loadData(): void {
    this.loading = true;
    this.error   = '';
    this.accessList = [];

    // Native timer: zone.js patches window.setTimeout so this always triggers
    // Angular's change detection, even if the RxJS chain somehow never settles.
    window.clearTimeout(this.loadTimer);
    this.loadTimer = window.setTimeout(() => {
      if (this.loading) {
        this.loading = false;
        this.error   = 'Chargement impossible (délai dépassé). Réessayez.';
      }
    }, 10_000);

    // timeout(7000) — converts a non-responding server (connection held open, no bytes sent)
    // into a TimeoutError after 7 s. catchError alone cannot handle this: it only fires
    // when the server sends an error response. Without timeout, a hanging request keeps
    // the forkJoin waiting forever, finalize never runs, and loading stays true.
    //
    // Two-tier error handling per call:
    //   TimeoutError  → re-throw so forkJoin errors → subscriber.error() shows the retry UI
    //   HTTP 4xx/5xx  → return of([])  so other calls can still succeed (graceful degradation)
    const withTimeout = <T>(obs: import('rxjs').Observable<T>) =>
      obs.pipe(
        timeout(7000),
        catchError(err => err instanceof TimeoutError ? throwError(() => err) : of([] as any))
      );

    forkJoin({
      grants: withTimeout(this.sharingService.getQueryGrants(this.query!.id)),
      users:  withTimeout(this.identityService.getAllUsers()),
      groups: withTimeout(this.groupService.getGroups())
    }).pipe(
      finalize(() => {
        window.clearTimeout(this.loadTimer);
        this.loading = false;
      }),
      takeUntil(this.destroy$)
    ).subscribe({
      next: ({ grants, users, groups }) => {
        this.buildAccessList(grants as any[]);
        this.buildPickerItems(users as any[], groups as any[]);
      },
      error: () => {
        // Reached only when at least one call timed out (server never responded).
        // HTTP errors are caught per-call above and returned as [].
        this.error = 'Chargement impossible — le serveur ne répond pas. Réessayez.';
      }
    });
  }

  retry(): void {
    this.loadData();
  }

  private buildAccessList(grants: ShareGrantDto[]): void {
    this.accessList = grants.map(g => {
      const isGroup = !!g.granteeGroupId;
      const name = g.granteeName || (isGroup ? 'Groupe' : 'Utilisateur');
      return {
        grantId: g.id,
        type: isGroup ? 'group' : 'user',
        name,
        initials: this.buildInitials(name),
        role: (g.accessLevel as 'READ' | 'EDIT' | 'OWNER') || 'READ',
        pendingRemove: false
      };
    });
  }

  private buildPickerItems(users: UserAccountDto[], groups: any[]): void {
    const userItems: PickerItem[] = users
      .filter(u => u.id)
      .map(u => ({
        id: u.id!,
        type: 'user' as const,
        name: u.displayName || u.username,
        initials: this.buildInitials(u.displayName || u.username)
      }));

    const groupItems: PickerItem[] = groups.map(g => ({
      id: g.id,
      type: 'group' as const,
      name: g.groupName,
      initials: this.buildInitials(g.groupName)
    }));

    this.pickerItems = [...userItems, ...groupItems];
    this.filteredPickerItems = [];
  }

  onPickerInput(): void {
    const q = this.pickerSearch.toLowerCase().trim();
    if (!q) { this.filteredPickerItems = []; return; }
    const pendingIds = new Set(this.accessList.filter(a => a.isPending).map(a => a.sourceId!));
    const grantedNames = new Set(this.accessList.filter(a => !a.pendingRemove).map(a => a.name));
    this.filteredPickerItems = this.pickerItems
      .filter(item =>
        item.name.toLowerCase().includes(q) &&
        !pendingIds.has(item.id) &&
        !grantedNames.has(item.name)
      )
      .slice(0, 8);
  }

  selectPickerItem(item: PickerItem): void {
    this.selectedPickerItem = item;
    this.pickerSearch = item.name;
    this.pickerOpen = false;
    this.filteredPickerItems = [];
  }

  addPending(): void {
    if (!this.selectedPickerItem) return;
    const item = this.selectedPickerItem;
    this.accessList = [...this.accessList, {
      grantId:       '',
      sourceId:      item.id,
      type:          item.type,
      name:          item.name,
      initials:      item.initials,
      role:          this.pendingRole,
      pendingRemove: false,
      isPending:     true
    }];
    this.resetPicker();
  }

  toggleRemove(p: AccessPerson): void {
    if (p.isPending) {
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
    if (!this.query) return;
    this.saving = true;
    this.error = '';

    const removes = this.accessList
      .filter(p => p.pendingRemove && !p.isPending)
      .map(p => this.sharingService.removeQueryGrant(this.query!.id, p.grantId));

    const adds = this.accessList
      .filter(p => p.isPending)
      .map(p => {
        const req: CreateShareGrantRequest = {
          shareLevel:     p.type === 'group' ? 'GROUP' : 'USERS',
          accessLevel:    p.role,
          granteeUserId:  p.type === 'user'  ? p.sourceId : undefined,
          granteeGroupId: p.type === 'group' ? p.sourceId : undefined
        };
        return this.sharingService.addQueryGrant(this.query!.id, req);
      });

    const allOps = [...removes, ...adds];

    if (allOps.length === 0) {
      this.logAudit();
      this.saving = false;
      this.onClose.emit();
      return;
    }

    forkJoin(allOps).pipe(
      finalize(() => { this.saving = false; })
    ).subscribe({
      next: () => {
        this.logAudit();
        this.onClose.emit();
      },
      error: () => {
        this.error = 'Erreur lors de la mise à jour des accès. Veuillez réessayer.';
      }
    });
  }

  private logAudit(): void {
    if (!this.query) return;
    this.auditService.logAuditEvent(
      'Partage de la requête',
      this.query.name,
      'QUERY',
      this.query.id
    );
  }

  private buildInitials(name: string): string {
    return (name || '?').split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  }
}
