import { DashboardService } from '@pages/dashboard/services/dashboard.service';
import { QueryService } from '@pages/query/services/query.service';
import { AuditService, AuditLogEntry } from '@pages/settings/services/audit.service';
import { UserService, UserProfile } from '@core/services/user.service';
import { AlertsService } from '@pages/alerts/services/alerts.service';
import { AlertEventRowComponent } from '@pages/alerts/components/alert-event-row.component';
import { ButtonComponent } from '@shared/components/ui/button.component';
import { AlertEvent } from '@core/models/types';
import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { SvgIconComponent } from '@shared/components/svg-icon/svg-icon.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterModule, SvgIconComponent, AlertEventRowComponent, ButtonComponent],
  template: `
    <div *ngIf="!bare; else bareLayout" class="flex h-full w-full bg-surface-muted dark:bg-black transition-colors overflow-hidden">
      <div
        *ngIf="isMobile && sidebarOpen"
        (click)="closeSidebarOnMobile()"
        class="fixed inset-0 z-40 bg-black/50 backdrop-blur-xs transition-opacity md:hidden"
      ></div>

      <aside
        [ngClass]="{
          'fixed inset-y-0 left-0 z-50 flex flex-col border-r border-line dark:border-zinc-800 bg-white dark:bg-zinc-900 transition-all duration-200 ease-in-out md:static md:z-auto': true,
          'w-64 translate-x-0': isMobile && sidebarOpen,
          '-translate-x-full w-64': isMobile && !sidebarOpen,
          'w-52': !isMobile && sidebarOpen,
          'w-16': !isMobile && !sidebarOpen
        }"
      >
        <div class="flex h-12 items-center border-b border-line dark:border-zinc-800 px-3 justify-between">
          <ng-container *ngIf="!sidebarOpen && !isMobile">
            <button
              type="button"
              (click)="toggleSidebar()"
              title="Agrandir la barre latérale"
              class="flex h-7 w-7 mx-auto items-center justify-center rounded-md text-ink-soft dark:text-zinc-300 hover:bg-surface-sunken dark:hover:bg-zinc-800 hover:text-ink dark:hover:text-white transition-colors cursor-pointer"
            >
              <app-svg-icon name="Menu" class="h-4 w-4"></app-svg-icon>
            </button>
          </ng-container>

          <ng-container *ngIf="sidebarOpen || isMobile">
            <div class="flex items-center gap-2 overflow-hidden">
              <div class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded bg-brand text-white">
                <app-svg-icon name="LayoutGrid" class="h-4 w-4"></app-svg-icon>
              </div>
              <div class="leading-tight truncate">
                <div class="text-sm font-semibold text-ink dark:text-white truncate">CockpitNG</div>
                <div class="text-2xs text-ink-faint dark:text-zinc-400 truncate">ProgesCode</div>
              </div>
            </div>

            <button
              type="button"
              (click)="toggleSidebar()"
              class="rounded-md p-1 text-ink-faint dark:text-zinc-400 hover:bg-surface-sunken dark:hover:bg-zinc-800 hover:text-ink dark:hover:text-white transition-colors cursor-pointer"
              [title]="isMobile ? 'Fermer le menu' : 'Réduire la barre latérale'"
            >
              <app-svg-icon [name]="isMobile ? 'X' : 'PanelLeft'" class="h-4 w-4"></app-svg-icon>
            </button>
          </ng-container>
        </div>

        <nav class="flex-1 space-y-1 p-2">
          <a
            routerLink="/"
            (click)="closeSidebarOnMobile()"
            routerLinkActive="bg-brand-soft text-brand-strong dark:bg-brand/20 dark:text-brand"
            [routerLinkActiveOptions]="{ exact: true }"
            class="flex items-center gap-2.5 rounded-md px-2.5 py-2 text-xs font-medium text-ink-soft dark:text-zinc-300 hover:bg-surface-sunken dark:hover:bg-zinc-800 hover:text-ink dark:hover:text-white transition-colors"
            [ngClass]="{ 'justify-center px-0': !sidebarOpen && !isMobile }"
            [title]="!sidebarOpen && !isMobile ? 'Tableaux de bord' : ''"
          >
            <app-svg-icon name="LayoutGrid" class="h-4 w-4 flex-shrink-0"></app-svg-icon>
            <span *ngIf="sidebarOpen || isMobile" class="truncate">Tableaux de bord</span>
          </a>

          <a
            routerLink="/requetes"
            (click)="closeSidebarOnMobile()"
            routerLinkActive="bg-brand-soft text-brand-strong dark:bg-brand/20 dark:text-brand"
            class="flex items-center gap-2.5 rounded-md px-2.5 py-2 text-xs font-medium text-ink-soft dark:text-zinc-300 hover:bg-surface-sunken dark:hover:bg-zinc-800 hover:text-ink dark:hover:text-white transition-colors"
            [ngClass]="{ 'justify-center px-0': !sidebarOpen && !isMobile }"
            [title]="!sidebarOpen && !isMobile ? 'Requêtes' : ''"
          >
            <app-svg-icon name="Layers" class="h-4 w-4 flex-shrink-0"></app-svg-icon>
            <span *ngIf="sidebarOpen || isMobile" class="truncate">Requêtes</span>
          </a>

          <a
            routerLink="/sources-de-donnees"
            (click)="closeSidebarOnMobile()"
            routerLinkActive="bg-brand-soft text-brand-strong dark:bg-brand/20 dark:text-brand"
            class="flex items-center gap-2.5 rounded-md px-2.5 py-2 text-xs font-medium text-ink-soft dark:text-zinc-300 hover:bg-surface-sunken dark:hover:bg-zinc-800 hover:text-ink dark:hover:text-white transition-colors"
            [ngClass]="{ 'justify-center px-0': !sidebarOpen && !isMobile }"
            [title]="!sidebarOpen && !isMobile ? 'Sources de données' : ''"
          >
            <app-svg-icon name="Database" class="h-4 w-4 flex-shrink-0"></app-svg-icon>
            <span *ngIf="sidebarOpen || isMobile" class="truncate">Sources de données</span>
          </a>

          <a
            routerLink="/admin-kpi"
            (click)="closeSidebarOnMobile()"
            routerLinkActive="bg-brand-soft text-brand-strong dark:bg-brand/20 dark:text-brand"
            class="flex items-center gap-2.5 rounded-md px-2.5 py-2 text-xs font-medium text-ink-soft dark:text-zinc-300 hover:bg-surface-sunken dark:hover:bg-zinc-800 hover:text-ink dark:hover:text-white transition-colors"
            [ngClass]="{ 'justify-center px-0': !sidebarOpen && !isMobile }"
            [title]="!sidebarOpen && !isMobile ? 'Console KPI' : ''"
          >
            <app-svg-icon name="BarChart3" class="h-4 w-4 flex-shrink-0"></app-svg-icon>
            <span *ngIf="sidebarOpen || isMobile" class="truncate">KPI administrateur</span>
          </a>

          <a
            routerLink="/alertes"
            (click)="closeSidebarOnMobile()"
            routerLinkActive="bg-brand-soft text-brand-strong dark:bg-brand/20 dark:text-brand"
            class="flex items-center gap-2.5 rounded-md px-2.5 py-2 text-xs font-medium text-ink-soft dark:text-zinc-300 hover:bg-surface-sunken dark:hover:bg-zinc-800 hover:text-ink dark:hover:text-white transition-colors"
            [ngClass]="{ 'justify-center px-0': !sidebarOpen && !isMobile }"
            [title]="!sidebarOpen && !isMobile ? 'Alertes' : ''"
          >
            <app-svg-icon name="BellRing" class="h-4 w-4 flex-shrink-0"></app-svg-icon>
            <span *ngIf="sidebarOpen || isMobile" class="truncate">Alertes</span>
          </a>

          <a
            routerLink="/parametres"
            (click)="closeSidebarOnMobile()"
            routerLinkActive="bg-brand-soft text-brand-strong dark:bg-brand/20 dark:text-brand"
            class="flex items-center gap-2.5 rounded-md px-2.5 py-2 text-xs font-medium text-ink-soft dark:text-zinc-300 hover:bg-surface-sunken dark:hover:bg-zinc-800 hover:text-ink dark:hover:text-white transition-colors"
            [ngClass]="{ 'justify-center px-0': !sidebarOpen && !isMobile }"
            [title]="!sidebarOpen && !isMobile ? 'Paramètres' : ''"
          >
            <app-svg-icon name="Settings" class="h-4 w-4 flex-shrink-0"></app-svg-icon>
            <span *ngIf="sidebarOpen || isMobile" class="truncate">Paramètres</span>
          </a>
        </nav>

        <div class="border-t border-line dark:border-zinc-800 p-2 bg-white dark:bg-zinc-900 mt-auto">
          <button
            class="flex w-full items-center gap-2 rounded-md p-1.5 text-left text-xs text-ink-soft dark:text-zinc-300 hover:bg-surface-sunken dark:hover:bg-zinc-800 cursor-pointer"
            [ngClass]="{ 'justify-center px-0': !sidebarOpen && !isMobile }"
          >
            <div class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-brand-soft text-2xs font-semibold text-brand-strong">
              {{ currentUser?.initials || 'AH' }}
            </div>
            <div *ngIf="sidebarOpen || isMobile" class="min-w-0 flex-1 leading-tight overflow-hidden">
              <div class="truncate font-medium text-ink dark:text-white">{{ currentUser?.displayName || 'Amine Haddad' }}</div>
              <div class="truncate text-2xs text-ink-faint dark:text-zinc-400">Créateur</div>
            </div>
            <app-svg-icon *ngIf="sidebarOpen || isMobile" name="ChevronDown" class="h-3.5 w-3.5 text-ink-faint dark:text-zinc-400 flex-shrink-0"></app-svg-icon>
          </button>
        </div>
      </aside>

      <div class="flex min-w-0 flex-1 flex-col">
        <header class="flex h-12 flex-shrink-0 items-center gap-3 border-b border-line dark:border-zinc-800 bg-white dark:bg-zinc-900 px-4 transition-colors">
          <button
            *ngIf="isMobile"
            type="button"
            (click)="toggleSidebar()"
            class="rounded-md p-1.5 text-ink-faint dark:text-zinc-300 hover:bg-surface-sunken dark:hover:bg-zinc-800 hover:text-ink dark:hover:text-white transition-colors cursor-pointer"
            title="Ouvrir le menu"
            aria-label="Toggle sidebar"
          >
            <app-svg-icon name="Menu" class="h-4 w-4"></app-svg-icon>
          </button>

          <div class="relative w-full max-w-xs md:w-[380px] flex items-center">
            <div class="pointer-events-none absolute left-2.5 flex items-center justify-center text-ink-faint dark:text-zinc-400">
              <app-svg-icon name="Search" class="h-3.5 w-3.5"></app-svg-icon>
            </div>
            <input
              placeholder="Rechercher un tableau de bord, une requête…"
              class="h-7 w-full rounded-md border border-line-strong dark:border-zinc-700 bg-surface-muted dark:bg-zinc-800 text-ink dark:text-white pr-2 text-xs outline-none focus:border-brand focus:bg-white dark:focus:bg-zinc-800 focus:ring-1 focus:ring-brand"
              style="padding-left: 2.25rem !important;"
            />
          </div>

          <div class="ml-auto flex items-center gap-1">
            <button
              type="button"
              (click)="toggleTheme()"
              [attr.aria-label]="isDark ? 'Mode clair' : 'Mode sombre'"
              [title]="isDark ? 'Passer en mode clair' : 'Passer en mode sombre'"
              class="rounded-md p-1.5 text-ink-faint dark:text-zinc-300 hover:bg-surface-sunken dark:hover:bg-zinc-800 hover:text-ink dark:hover:text-white transition-colors cursor-pointer"
            >
              <app-svg-icon [name]="isDark ? 'Sun' : 'Moon'" class="h-4 w-4"></app-svg-icon>
            </button>

            <button
              type="button"
              aria-label="Aide"
              class="rounded-md p-1.5 text-ink-faint dark:text-zinc-300 hover:bg-surface-sunken dark:hover:bg-zinc-800 hover:text-ink dark:hover:text-white transition-colors cursor-pointer"
            >
              <app-svg-icon name="HelpCircle" class="h-4 w-4"></app-svg-icon>
            </button>

            <div class="relative">
              <button
                type="button"
                [attr.aria-label]="notificationsAriaLabel"
                [attr.aria-expanded]="feedOpen"
                (click)="toggleFeed()"
                class="relative rounded-md p-1.5 text-ink-faint dark:text-zinc-300 hover:bg-surface-sunken dark:hover:bg-zinc-800 hover:text-ink dark:hover:text-white transition-colors cursor-pointer"
              >
                <app-svg-icon name="Bell" class="h-4 w-4"></app-svg-icon>
                <span
                  *ngIf="activeAlertCount > 0"
                  class="absolute right-0.5 top-0.5 flex h-3.5 min-w-3.5 items-center justify-center rounded-full bg-negative px-0.5 text-[8px] font-bold leading-none text-white"
                >{{ activeAlertBadgeLabel }}</span>
              </button>

              <section
                *ngIf="feedOpen"
                class="absolute right-0 top-10 z-40 w-[min(390px,calc(100vw-2rem))] overflow-hidden rounded-lg border border-line bg-surface-muted shadow-pop"
              >
                <div class="flex items-center gap-2 border-b border-line bg-white px-3 py-2.5">
                  <app-svg-icon name="BellRing" class="h-4 w-4 text-brand-strong"></app-svg-icon>
                  <div class="flex-1">
                    <h2 class="text-xs font-semibold text-ink">Notifications</h2>
                    <p class="text-2xs text-ink-faint">{{ liveAlertsCountLabel }}</p>
                  </div>
                  <button
                    *ngIf="liveAlerts.length > 0"
                    (click)="acknowledgeAll()"
                    class="inline-flex items-center gap-1 text-2xs font-medium text-brand"
                  >
                    <app-svg-icon name="CheckCheck" class="h-3.5 w-3.5"></app-svg-icon>
                    Tout accuser
                  </button>
                </div>
                <div class="max-h-[420px] space-y-2 overflow-auto p-2">
                  <app-alert-event-row
                    *ngFor="let alert of liveAlerts"
                    [alert]="alert"
                    [compact]="true"
                    (onAcknowledge)="acknowledgeAlert($event)"
                    (onSnooze)="snoozeAlert($event)"
                  ></app-alert-event-row>
                  <div *ngIf="!liveAlerts.length" class="flex flex-col items-center justify-center gap-2 p-6 text-center">
                    <app-svg-icon name="BellRing" class="h-5 w-5 text-ink-faint"></app-svg-icon>
                    <p class="text-xs font-semibold text-ink">Aucune alerte en cours</p>
                    <p class="text-2xs text-ink-faint">Les nouvelles alertes apparaîtront ici.</p>
                  </div>
                </div>
                <div class="border-t border-line bg-white p-2">
                  <app-button size="sm" variant="ghost" customClass="w-full" (onClick)="goToAlertsCenter()">
                    <app-svg-icon name="ArrowRight" class="h-3.5 w-3.5"></app-svg-icon>
                    Voir le centre d'alertes
                  </app-button>
                </div>
              </section>
            </div>
          </div>
        </header>

        <main class="min-h-0 flex-1 overflow-auto bg-surface-muted dark:bg-black transition-colors">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>

    <ng-template #bareLayout>
      <router-outlet></router-outlet>
    </ng-template>
  `
})
export class AppShellComponent implements OnInit {
  bare: boolean = false;
  isDark: boolean = false;
  sidebarOpen: boolean = true;
  isMobile: boolean = false;
  currentUser: UserProfile | null = null;

  feedOpen = false;
  alerts: AlertEvent[] = [];

  constructor(
    private router: Router,
    private dashboardService: DashboardService,
    private queryService: QueryService,
    private auditService: AuditService,
    private userService: UserService,
    private alertsService: AlertsService
  ) {
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd))
      .subscribe((e: any) => {
        const url = e.urlAfterRedirects || e.url;
        this.bare = /^\/(tableau|editeur)/.test(url);
        if (this.isMobile) {
          this.sidebarOpen = false;
        }
        this.feedOpen = false;
      });
  }

  ngOnInit(): void {
    this.checkScreenSize();
    this.isDark = document.documentElement.classList.contains('dark') || localStorage.getItem('theme') === 'dark';
    this.applyTheme();
    this.userService.currentUser$.subscribe((user) => {
      this.currentUser = user;
    });

    this.alertsService.loadAlerts();
    this.alertsService.alerts$.subscribe((alerts) => {
      this.alerts = alerts;
    });
  }

  get activeAlertCount(): number {
    return this.alerts.filter((alert) => alert.status === 'active').length;
  }

  get liveAlerts(): AlertEvent[] {
    return this.alerts.filter((alert) => alert.status !== 'resolved').slice(0, 4);
  }

  get activeAlertBadgeLabel(): string {
    return this.activeAlertCount > 9 ? '9+' : String(this.activeAlertCount);
  }

  get liveAlertsCountLabel(): string {
    return `${this.liveAlerts.length} alerte${this.liveAlerts.length > 1 ? 's' : ''} à suivre`;
  }

  get notificationsAriaLabel(): string {
    return 'Notifications' + (this.activeAlertCount ? ', ' + this.activeAlertCount + ' à traiter' : '');
  }

  toggleFeed(): void {
    this.feedOpen = !this.feedOpen;
  }

  acknowledgeAlert(id: string): void {
    this.alertsService.acknowledgeAlert(id);
  }

  snoozeAlert(payload: { id: string; minutes: number }): void {
    this.alertsService.snoozeAlert(payload.id, payload.minutes);
  }

  acknowledgeAll(): void {
    this.liveAlerts.forEach((alert) => this.alertsService.acknowledgeAlert(alert.id));
  }

  goToAlertsCenter(): void {
    this.feedOpen = false;
    this.router.navigateByUrl('/alertes');
  }

  @HostListener('window:resize')
  onResize(): void {
    this.checkScreenSize();
  }

  private checkScreenSize(): void {
    const wasMobile = this.isMobile;
    this.isMobile = window.innerWidth < 768;
    if (this.isMobile && !wasMobile) {
      this.sidebarOpen = false;
    } else if (!this.isMobile && wasMobile) {
      this.sidebarOpen = true;
    }
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebarOnMobile(): void {
    if (this.isMobile) {
      this.sidebarOpen = false;
    }
  }

  toggleTheme(): void {
    this.isDark = !this.isDark;
    localStorage.setItem('theme', this.isDark ? 'dark' : 'light');
    this.applyTheme();
  }

  private applyTheme(): void {
    if (this.isDark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }
}