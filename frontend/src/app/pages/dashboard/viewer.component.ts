import { DashboardService } from '@pages/dashboard/services/dashboard.service';
import { QueryService } from '@pages/query/services/query.service';
import { AuditService, AuditLogEntry } from '@pages/settings/services/audit.service';
import { UserService, UserProfile } from '@core/services/user.service';
import { AnalyticsService } from '@pages/admin-kpi/services/analytics.service';
import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { Dashboard, Widget } from '@core/models/types';
import { refreshToMs } from '@core/services/utils';
import {
  resolveDashboardRuntimeFilters,
  activeDashboardFilters,
  RuntimeQueryFilter,
  ActiveDashboardFilter
} from '@pages/dashboard/services/dashboard-filters.service';
import { ButtonComponent } from '@shared/components/ui/button.component';
import { BadgeComponent } from '@shared/components/ui/badge.component';
import { ModalComponent } from '@shared/components/ui/modal.component';
import { SvgIconComponent } from '@shared/components/svg-icon/svg-icon.component';
import { DashboardGridComponent } from '@pages/dashboard/components/grid/dashboard-grid.component';
import { WidgetCardComponent } from '@pages/dashboard/components/widgets/widget-card.component';
import { RawDataModalComponent } from '@pages/dashboard/components/widgets/raw-data-modal.component';
import { FilterBarComponent } from '@pages/dashboard/components/viewer/filter-bar.component';
import { RefreshControlComponent } from '@pages/dashboard/components/viewer/refresh-control.component';
import { ShareModalComponent } from '@pages/dashboard/components/home/share-modal.component';
@Component({
  selector: 'app-dashboard-viewer',
  standalone: true,
  imports: [
    CommonModule,
    ButtonComponent,
    BadgeComponent,
    ModalComponent,
    SvgIconComponent,
    DashboardGridComponent,
    WidgetCardComponent,
    RawDataModalComponent,
    FilterBarComponent,
    RefreshControlComponent,
    ShareModalComponent
  ],
  template: `
    <div *ngIf="loading" class="flex h-full items-center justify-center text-xs text-ink-faint gap-2">
      <div class="h-4 w-4 animate-spin rounded-full border-2 border-brand border-t-transparent"></div>
      Chargement du tableau de bord...
    </div>
    <div *ngIf="!loading && dashboard; else notFound" class="flex h-full w-full flex-col bg-surface-muted">
      <header *ngIf="!kiosk" class="flex min-h-12 flex-shrink-0 flex-wrap sm:flex-nowrap items-center justify-between gap-2 border-b border-line bg-white px-3 py-2 sm:px-4 sm:py-0">
        <div class="flex items-center gap-2 min-w-0 flex-1 sm:flex-initial">
          <app-button variant="ghost" size="sm" (onClick)="navigateHome()" class="flex-shrink-0">
            <app-svg-icon name="ArrowLeft" class="h-4 w-4 mr-1"></app-svg-icon>
            <span class="hidden sm:inline">Retour</span>
          </app-button>
          <span class="h-4 w-1 flex-shrink-0 rounded" [style.backgroundColor]="dashboard.color"></span>
          <h1 class="text-xs sm:text-sm font-semibold text-ink truncate max-w-[150px] xs:max-w-[220px] sm:max-w-none">
            {{ dashboard.name }}
          </h1>
          <app-badge *ngIf="dashboard.status === 'draft'" tone="caution" class="flex-shrink-0">
            Brouillon
          </app-badge>
        </div>
        <div class="ml-auto flex items-center gap-1 sm:gap-1.5 flex-shrink-0">
          <app-refresh-control
            [interval]="dashboard.refreshInterval"
            [countdown]="countdown"
            [paused]="paused"
            (onTogglePause)="paused = !paused"
            (onRefresh)="refreshNow()"
          ></app-refresh-control>
          <app-button variant="secondary" size="sm" (onClick)="kiosk = true" class="hidden sm:inline-flex">
            Kiosque
          </app-button>
          <app-button variant="secondary" size="sm" (onClick)="showShare = true">
            Partager
          </app-button>
          <app-button variant="primary" size="sm" (onClick)="editDashboard()">
            <app-svg-icon name="Settings" class="h-3.5 w-3.5 sm:mr-1"></app-svg-icon>
            <span class="hidden sm:inline">Modifier</span>
          </app-button>
        </div>
      </header>
      <button
        *ngIf="kiosk"
        (click)="kiosk = false"
        class="fixed right-3 top-3 z-50 rounded-md border border-line bg-white px-2 py-1 text-2xs text-ink-soft shadow-card hover:bg-surface-muted"
      >
        Quitter le mode kiosque
      </button>
      <app-filter-bar
        *ngIf="!kiosk"
        [filters]="dashboard.globalFilters"
        [values]="filterValues"
        (onChange)="updateFilter($event.id, $event.value)"
        (onReset)="resetFilters()"
      ></app-filter-bar>
      <div class="min-h-0 flex-1 overflow-auto p-3">
        <div class="mx-auto max-w-[1500px]">
          <app-dashboard-grid
            [columns]="dashboard.columns"
            [widgets]="dashboard.widgets"
            [editable]="false"
            [widgetTemplate]="widgetTmpl"
          ></app-dashboard-grid>
          <ng-template #widgetTmpl let-widget>
            <app-widget-card
              [widget]="widget"
              [dashboard]="dashboard!"
              [refreshTick]="tick"
              [runtimeFilters]="runtimeFilters"
              (onViewData)="rawWidget = $event"
              (onMaximize)="maxWidget = $event"
            ></app-widget-card>
          </ng-template>
        </div>
      </div>
      <app-raw-data-modal
        [widget]="rawWidget"
        [dashboard]="dashboard"
        [open]="!!rawWidget"
        (onClose)="rawWidget = null"
        [runtimeFilters]="runtimeFilters"
        [activeFilters]="activeFilters"
      ></app-raw-data-modal>
      <app-modal
        [open]="!!maxWidget"
        (onClose)="maxWidget = null"
        [title]="maxWidget?.title || ''"
        width="max-w-4xl"
      >
        <div class="h-[60vh]">
          <app-widget-card
            *ngIf="maxWidget"
            [widget]="maxWidget"
            [dashboard]="dashboard"
            [refreshTick]="tick"
            [runtimeFilters]="runtimeFilters"
          ></app-widget-card>
        </div>
      </app-modal>
      <app-share-modal
        [dashboard]="dashboard"
        [open]="showShare"
        (onClose)="showShare = false"
      ></app-share-modal>
    </div>
    <ng-template #notFound>
      <div *ngIf="!loading" class="flex h-full items-center justify-center text-sm text-ink-faint">
        Tableau de bord introuvable.
        <app-button class="ml-2" (onClick)="navigateHome()">Retour</app-button>
      </div>
    </ng-template>
  `
})
export class DashboardViewerComponent implements OnInit, OnDestroy {
  dashboard: Dashboard | null = null;
  loading: boolean = true;
  filterValues: Record<string, string> = {};
  rawWidget: Widget | null = null;
  maxWidget: Widget | null = null;
  showShare: boolean = false;
  kiosk: boolean = false;
  tick: number = 0;
  countdown: number = 0;
  paused: boolean = false;
  private timer: any;
  sub!: Subscription;
  private dashboardsSub!: Subscription;
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private dashboardService: DashboardService, private queryService: QueryService, private auditService: AuditService, private userService: UserService,
    private analyticsService: AnalyticsService,
    private cdr: ChangeDetectorRef
  ) {}
  ngOnInit(): void {
    this.loading = true;
    this.sub = this.route.params.subscribe((params) => {
      const id = params['id'];
      this.dashboardsSub?.unsubscribe();
      this.dashboardsSub = this.dashboardService.dashboards$.subscribe((list) => {
        const found = list.find((d) => d.id === id);
        const prevInterval = this.dashboard?.refreshInterval;
        const isFirstLoad = !this.dashboard || this.dashboard.id !== id;
        this.dashboard = found ?? null;
        this.loading = false;
        if (found) {
          if (isFirstLoad) {
            this.analyticsService.track('dashboard_view', 'dashboard', found.id, found.name, { dashboardId: found.id, dashboardName: found.name });
          }
          if (!Object.keys(this.filterValues).length) {
            this.filterValues = Object.fromEntries(
              found.globalFilters.map((f) => [f.id, f.defaultValue || 'TOUS'])
            );
          }
          if (!this.timer || found.refreshInterval !== prevInterval) {
            this.setupRefreshTimer();
          }
        }
        try {
          this.cdr.detectChanges();
        } catch {}
      });
    });
  }
  ngOnDestroy(): void {
    if (this.sub) this.sub.unsubscribe();
    if (this.dashboardsSub) this.dashboardsSub.unsubscribe();
    if (this.timer) clearInterval(this.timer);
  }
  get runtimeFilters(): RuntimeQueryFilter[] {
    return this.dashboard
      ? resolveDashboardRuntimeFilters(this.dashboard.globalFilters, this.filterValues)
      : [];
  }
  get activeFilters(): ActiveDashboardFilter[] {
    return this.dashboard
      ? activeDashboardFilters(this.dashboard.globalFilters, this.filterValues)
      : [];
  }
  updateFilter(id: string, val: string) {
    this.filterValues = { ...this.filterValues, [id]: val };
  }
  resetFilters() {
    if (!this.dashboard) return;
    this.filterValues = Object.fromEntries(
      this.dashboard.globalFilters.map((f) => [f.id, 'TOUS'])
    );
  }
  navigateHome() {
    this.router.navigate(['/']);
  }
  editDashboard() {
    if (this.dashboard) {
      this.router.navigate(['/editeur', this.dashboard.id]);
    }
  }
  setupRefreshTimer() {
    if (this.timer) clearInterval(this.timer);
    this.timer = null;
    if (!this.dashboard || !this.dashboard.refreshInterval || this.dashboard.refreshInterval === 'off') {
      this.countdown = 0;
      return;
    }
    const ms = refreshToMs(this.dashboard.refreshInterval);
    if (!ms) {
      this.countdown = 0;
      return;
    }
    this.countdown = Math.round(ms / 1000);
    this.timer = setInterval(() => {
      if (this.paused || !this.dashboard) return;
      if (this.countdown <= 0) {
        this.tick++;
        const currentMs = refreshToMs(this.dashboard.refreshInterval);
        this.countdown = Math.round(currentMs / 1000);
      } else {
        this.countdown--;
      }
      try {
        this.cdr.detectChanges();
      } catch {}
    }, 1000);
  }
  refreshNow() {
    this.tick++;
    if (this.dashboard && this.dashboard.refreshInterval && this.dashboard.refreshInterval !== 'off') {
      const ms = refreshToMs(this.dashboard.refreshInterval);
      this.countdown = Math.round(ms / 1000);
    }
    try {
      this.cdr.detectChanges();
    } catch {}
  }
}
