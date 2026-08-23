import { DashboardService } from '@pages/dashboard/services/dashboard.service';
import { QueryService } from '@pages/query/services/query.service';
import { AuditService, AuditLogEntry } from '@pages/settings/services/audit.service';
import { UserService, UserProfile } from '@core/services/user.service';
import { PermissionService } from '@core/services/permission.service';
import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { Dashboard } from '@core/models/types';
import { ButtonComponent } from '@shared/components/ui/button.component';
import { SvgIconComponent } from '@shared/components/svg-icon/svg-icon.component';
import { DashboardCardComponent } from '@pages/dashboard/components/home/dashboard-card.component';
import { DashboardListRowComponent } from '@pages/dashboard/components/home/dashboard-list-row.component';
import { NewDashboardModalComponent } from '@pages/dashboard/components/home/new-dashboard-modal.component';
type Tab = 'mine' | 'shared' | 'recent' | 'favorites' | 'archived';
type Sort = 'name' | 'created' | 'updated';
@Component({
  selector: 'app-dashboard-home',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    SvgIconComponent,
    DashboardCardComponent,
    DashboardListRowComponent,
    NewDashboardModalComponent
  ],
  templateUrl: './dashboard.component.html'
})
export class DashboardHomeComponent implements OnInit, OnDestroy {
  dashboards: Dashboard[] = [];
  sub!: Subscription;
  currentTab: Tab = 'mine';
  currentView: 'grid' | 'list' = 'grid';
  currentSort: Sort = 'updated';
  search: string = '';
  showNewModal: boolean = false;
  tabs: { id: Tab; label: string }[] = [
    { id: 'mine', label: 'Mes tableaux de bord' },
    { id: 'shared', label: 'Partagés avec moi' },
    { id: 'recent', label: 'Récents' },
    { id: 'favorites', label: 'Favoris' },
    { id: 'archived', label: 'Archives' }
  ];
  constructor(
    private dashboardService: DashboardService, private queryService: QueryService, private auditService: AuditService, private userService: UserService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    public permissionService: PermissionService
  ) {}
  ngOnInit(): void {
    this.dashboardService.loadFromBackend();
    this.queryService.loadFromBackend();
    this.sub = this.dashboardService.dashboards$.subscribe((list) => {
      this.dashboards = list;
      this.cdr.markForCheck();
    });
  }
  ngOnDestroy(): void {
    if (this.sub) this.sub.unsubscribe();
  }
  get filteredDashboards(): Dashboard[] {
    let list = this.dashboards;
    if (this.currentTab === 'archived') {
      list = list.filter((d) => d.archived);
    } else {
      list = list.filter((d) => !d.archived);
      if (this.currentTab === 'mine') list = list.filter((d) => !d.sharedWithMe);
      if (this.currentTab === 'shared') list = list.filter((d) => d.sharedWithMe);
      if (this.currentTab === 'favorites') list = list.filter((d) => d.favorite);
    }
    if (this.search.trim()) {
      const q = this.search.toLowerCase();
      list = list.filter(
        (d) =>
          d.name.toLowerCase().includes(q) ||
          d.tags.some((t) => t.toLowerCase().includes(q))
      );
    }
    const sorted = [...list].sort((a, b) => {
      if (this.currentSort === 'name') return a.name.localeCompare(b.name);
      if (this.currentSort === 'created') return b.createdAt.localeCompare(a.createdAt);
      return b.updatedAt.localeCompare(a.updatedAt);
    });
    return this.currentTab === 'recent' ? sorted.slice(0, 6) : sorted;
  }
  openDashboard(d: Dashboard) {
    this.router.navigate(['/tableau', d.id]);
  }
}
