import {
  Component, Input, Output, EventEmitter,
  OnChanges, OnDestroy, SimpleChanges
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { Dashboard } from '@core/models/types';
import { SharingService, ShareGrantDto } from '@core/services/sharing.service';
import { SvgIconComponent } from '@shared/components/svg-icon/svg-icon.component';

interface GrantRow {
  type: 'user' | 'group';
  name: string;
  initials: string;
  role: 'READ' | 'EDIT' | 'OWNER';
}

const SHARE_LABELS: Record<string, string> = {
  private:      'Privé',
  users:        'Utilisateurs',
  group:        'Groupe',
  organization: 'Organisation'
};
const SHARE_ICONS: Record<string, string> = {
  private:      'Lock',
  users:        'Users',
  group:        'Users',
  organization: 'Globe'
};
const ROLE_LABELS: Record<string, string> = {
  READ:  'Lecture',
  EDIT:  'Édition',
  OWNER: 'Propriétaire'
};

@Component({
  selector: 'app-dashboard-access-popover',
  standalone: true,
  imports: [CommonModule, SvgIconComponent],
  template: `
    <!-- Trigger button — styled like the old plain label but now clickable -->
    <button
      type="button"
      (click)="toggle()"
      class="inline-flex items-center gap-1 rounded px-1 py-0.5 text-2xs text-ink-faint
             hover:bg-surface-sunken hover:text-ink transition-colors"
      [title]="shareLevel === 'private' ? 'Privé — visible par vous uniquement'
                                        : 'Voir qui a accès'"
    >
      <app-svg-icon [name]="shareIcon" class="h-3.5 w-3.5"></app-svg-icon>
      <span>{{ shareLabel }}</span>
      <app-svg-icon *ngIf="shareLevel !== 'private'"
        name="ChevronDown"
        class="h-2.5 w-2.5 opacity-60"
        [class.rotate-180]="open"
      ></app-svg-icon>
    </button>

    <!-- Backdrop -->
    <div *ngIf="open" class="fixed inset-0 z-[45]" (click)="open = false"></div>

    <!-- Popover panel -->
    <div *ngIf="open"
      class="absolute bottom-full left-0 z-[46] mb-2 w-64 rounded-xl border border-line bg-white shadow-pop overflow-hidden"
    >
      <!-- Header -->
      <div class="flex items-center justify-between border-b border-line bg-surface-muted/50 px-3 py-2">
        <span class="text-[11px] font-bold uppercase tracking-wider text-ink-faint">
          Accès partagés
        </span>
        <button type="button" (click)="open = false"
          class="rounded p-0.5 text-ink-faint hover:text-ink transition-colors">
          <app-svg-icon name="X" class="h-3.5 w-3.5"></app-svg-icon>
        </button>
      </div>

      <!-- Loading -->
      <div *ngIf="loading" class="flex items-center justify-center py-6 gap-2">
        <div class="h-4 w-4 animate-spin rounded-full border-2 border-brand border-t-transparent"></div>
        <span class="text-xs text-ink-faint">Chargement…</span>
      </div>

      <!-- Error -->
      <div *ngIf="!loading && error"
        class="px-3 py-3 text-[11px] text-negative text-center">
        {{ error }}
      </div>

      <!-- Body -->
      <div *ngIf="!loading && !error" class="max-h-52 overflow-y-auto">

        <!-- Organisation-wide access -->
        <div *ngIf="shareLevel === 'organization'"
          class="flex items-center gap-2.5 px-3 py-2.5 border-b border-line">
          <div class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-green-100">
            <app-svg-icon name="Globe" class="h-3.5 w-3.5 text-green-700"></app-svg-icon>
          </div>
          <div class="min-w-0 flex-1">
            <div class="text-xs font-semibold text-ink truncate">Toute l'organisation</div>
            <div class="text-[10px] text-ink-faint">Tous les utilisateurs authentifiés</div>
          </div>
          <span class="shrink-0 rounded-full bg-blue-50 px-2 py-0.5 text-[10px] font-semibold text-brand-strong">
            Lecture
          </span>
        </div>

        <!-- Individual / group grants -->
        <div *ngFor="let g of grants; let last = last"
          class="flex items-center justify-between gap-2 px-3 py-2.5"
          [class.border-b]="!last"
          [class.border-line]="!last"
        >
          <div class="flex items-center gap-2 min-w-0">
            <div class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-[10px] font-bold"
              [ngClass]="g.type === 'group'
                ? 'bg-purple-100 text-purple-700'
                : 'bg-slate-100 text-slate-600'"
            >
              {{ g.initials }}
            </div>
            <div class="min-w-0">
              <div class="truncate text-xs font-semibold text-ink">{{ g.name }}</div>
              <div class="text-[10px] text-ink-faint">
                {{ g.type === 'group' ? 'Groupe' : 'Utilisateur' }}
              </div>
            </div>
          </div>
          <span class="shrink-0 rounded-full px-2 py-0.5 text-[10px] font-semibold"
            [ngClass]="roleBadgeClass(g.role)">
            {{ roleLabel(g.role) }}
          </span>
        </div>

        <!-- No individual grants -->
        <div *ngIf="grants.length === 0 && shareLevel !== 'organization'"
          class="py-5 text-center text-[11px] text-ink-faint italic">
          Aucun accès individuel configuré.
        </div>
      </div>

      <!-- Footer: manage link (only for users who can share) -->
      <div *ngIf="canShare"
        class="border-t border-line bg-surface-muted/30 px-3 py-2">
        <button type="button" (click)="manage()"
          class="inline-flex items-center gap-1 text-[11px] font-semibold text-brand hover:underline">
          <app-svg-icon name="Settings" class="h-3 w-3"></app-svg-icon>
          Gérer les accès
        </button>
      </div>
    </div>
  `
})
export class DashboardAccessPopoverComponent implements OnChanges, OnDestroy {
  @Input() dashboard!: Dashboard;
  /** True when the current user has the share permission on this dashboard. */
  @Input() canShare = false;
  /** Emitted when the user clicks "Gérer les accès" — parent should open the edit modal. */
  @Output() onManage = new EventEmitter<void>();

  open    = false;
  loading = false;
  error   = '';
  grants: GrantRow[] = [];

  private destroy$   = new Subject<void>();
  private loadTimer?: number;

  constructor(private sharingService: SharingService) {}

  // ── Angular lifecycle ──────────────────────────────────────────────────────

  ngOnChanges(changes: SimpleChanges): void {
    // If the dashboard input changes, clear the displayed data.
    if (changes['dashboard'] && !changes['dashboard'].firstChange) {
      this.grants = [];
      this.error  = '';
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    window.clearTimeout(this.loadTimer);
  }

  // ── Getters ────────────────────────────────────────────────────────────────

  get shareLevel(): string { return this.dashboard?.shareLevel || 'private'; }
  get shareLabel(): string { return SHARE_LABELS[this.shareLevel] || this.shareLevel; }
  get shareIcon():  string { return SHARE_ICONS[this.shareLevel]  || 'Users'; }

  // ── Actions ────────────────────────────────────────────────────────────────

  toggle(): void {
    // Private dashboards have nothing to show — skip.
    if (this.shareLevel === 'private') return;

    this.open = !this.open;
    // Always re-fetch on open — no cache, so data stays fresh after sharing.
    if (this.open && !this.loading) {
      this.loadGrants();
    }
  }

  manage(): void {
    this.open = false;
    this.onManage.emit();
  }

  // ── Data loading ───────────────────────────────────────────────────────────

  private loadGrants(): void {
    this.loading = true;
    this.error   = '';

    window.clearTimeout(this.loadTimer);
    this.loadTimer = window.setTimeout(() => {
      if (this.loading) {
        this.loading = false;
        this.error   = 'Délai dépassé. Réessayez.';
      }
    }, 10_000);

    this.sharingService.getDashboardGrants(this.dashboard.id)
      .pipe(
        finalize(() => {
          window.clearTimeout(this.loadTimer);
          this.loading = false;
        }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (grants) => {
          this.grants = grants.map(g => {
            const isGroup = !!g.granteeGroupId;
            const name    = g.granteeName || (isGroup ? 'Groupe' : 'Utilisateur');
            return {
              type:     isGroup ? 'group' : 'user',
              name,
              initials: this.initials(name),
              role:     (g.accessLevel as 'READ' | 'EDIT' | 'OWNER') || 'READ'
            };
          });
        },
        error: () => {
          this.error = 'Impossible de charger les accès.';
        }
      });
  }

  // ── Display helpers ────────────────────────────────────────────────────────

  roleBadgeClass(role: string): string {
    if (role === 'OWNER') return 'bg-purple-100 text-purple-700';
    if (role === 'EDIT')  return 'bg-amber-100  text-amber-700';
    return 'bg-blue-50 text-brand-strong';
  }

  roleLabel(role: string): string {
    return ROLE_LABELS[role] || role;
  }

  private initials(name: string): string {
    return (name || '?').split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  }
}
