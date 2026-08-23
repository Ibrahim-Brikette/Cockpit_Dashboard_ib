import { DashboardService } from '@pages/dashboard/services/dashboard.service';
import { QueryService } from '@pages/query/services/query.service';
import { AuditService, AuditLogEntry } from '@pages/settings/services/audit.service';
import { UserService, UserProfile } from '@core/services/user.service';
import { PermissionService } from '@core/services/permission.service';
import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { DataQuery } from '@core/models/types';
import { relativeDate } from '@core/services/utils';
import { ButtonComponent } from '@shared/components/ui/button.component';
import { BadgeComponent } from '@shared/components/ui/badge.component';
import { SvgIconComponent } from '@shared/components/svg-icon/svg-icon.component';
import { QueryWizardModalComponent } from '@pages/query/components/query-wizard-modal.component';
import { QueryShareModalComponent } from '@pages/query/components/query-share-modal.component';
import { ConfirmModalComponent } from '@shared/components/ui/confirm-modal.component';
@Component({
  selector: 'app-query-catalog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    SvgIconComponent,
    QueryWizardModalComponent,
    QueryShareModalComponent,
    ConfirmModalComponent
  ],
  templateUrl: './query.component.html'
})
export class QueryCatalogComponent implements OnInit, OnDestroy {
  queries: DataQuery[] = [];
  search: string = '';
  showWizard: boolean = false;
  editingQuery: DataQuery | undefined;
  deleteConfirmOpen: boolean = false;
  queryToDeleteId: string | null = null;
  shareOpen: boolean = false;
  shareTarget: DataQuery | null = null;
  currentPage: number = 1;
  pageSize: number = 10;
  sortField: string = 'updatedAt';
  sortDirection: 'asc' | 'desc' = 'desc';
  sources: any[] = [];
  sub!: Subscription;
  sourcesSub!: Subscription;
  constructor(
    private dashboardService: DashboardService, private queryService: QueryService, private auditService: AuditService, private userService: UserService,
    private cdr: ChangeDetectorRef,
    public permissionService: PermissionService
  ) {}
  ngOnInit(): void {
    this.dashboardService.loadFromBackend();
    this.queryService.loadFromBackend();
    this.sourcesSub = this.queryService.catalogSources$.subscribe((srcs) => {
      if (srcs && srcs.length > 0) {
        this.sources = srcs;
        this.cdr.markForCheck();
      }
    });
    this.sub = this.queryService.queries$.subscribe((list) => {
      this.queries = list;
      this.currentPage = 1;
      this.cdr.markForCheck();
    });
  }
  ngOnDestroy(): void {
    if (this.sub) this.sub.unsubscribe();
    if (this.sourcesSub) this.sourcesSub.unsubscribe();
  }
  toggleSort(field: string): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }
    this.currentPage = 1;
  }
  get filteredQueries(): any[] {
    const list = this.queries.map((q) => ({
      ...q,
      sources: this.getSourcesList(q),
      modified: relativeDate(q.updatedAt)
    }));
    if (!this.search.trim()) return list;
    const searchLower = this.search.toLowerCase();
    return list.filter(
      (item) =>
        item.name.toLowerCase().includes(searchLower) ||
        (item.description && item.description.toLowerCase().includes(searchLower)) ||
        (item.sources && item.sources.some((s: string) => s.toLowerCase().includes(searchLower)))
    );
  }
  get sortedQueries(): any[] {
    const list = this.filteredQueries;
    if (!this.sortField) return list;
    const field = this.sortField;
    const dir = this.sortDirection === 'asc' ? 1 : -1;
    return [...list].sort((a, b) => {
      let valA: any = a[field];
      let valB: any = b[field];
      if (field === 'sources') {
        valA = (a.sources || []).join(', ');
        valB = (b.sources || []).join(', ');
      } else if (field === 'usedByWidgets') {
        valA = Number(valA) || 0;
        valB = Number(valB) || 0;
        return (valA - valB) * dir;
      } else if (field === 'updatedAt') {
        valA = new Date(valA || 0).getTime();
        valB = new Date(valB || 0).getTime();
        return (valA - valB) * dir;
      }
      valA = (valA || '').toString().toLowerCase();
      valB = (valB || '').toString().toLowerCase();
      return valA.localeCompare(valB) * dir;
    });
  }
  get totalPages(): number {
    return Math.ceil(this.sortedQueries.length / this.pageSize) || 1;
  }
  get paginatedQueries(): any[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.sortedQueries.slice(start, start + this.pageSize);
  }
  get startIndex(): number {
    return (this.currentPage - 1) * this.pageSize + 1;
  }
  get endIndex(): number {
    return Math.min(this.currentPage * this.pageSize, this.sortedQueries.length);
  }
  get pageNumbers(): number[] {
    const total = this.totalPages;
    const current = this.currentPage;
    const pages: number[] = [];
    const maxButtons = 5;
    let start = Math.max(1, current - Math.floor(maxButtons / 2));
    let end = Math.min(total, start + maxButtons - 1);
    if (end - start + 1 < maxButtons) {
      start = Math.max(1, end - maxButtons + 1);
    }
    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    return pages;
  }
  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }
  prevPage(): void {
    this.goToPage(this.currentPage - 1);
  }
  nextPage(): void {
    this.goToPage(this.currentPage + 1);
  }
  getSourcesList(q: any): string[] {
    const rawIds: string[] = q.sourceIds || (Array.isArray(q.sources) ? q.sources.map((s: any) => typeof s === 'string' ? s : (s.id || s.sourceKey || s.key)) : []);
    return (rawIds || []).map((id: string) => {
      const src = this.sources.find((s) => s.id === id || s.key === id || s.sourceKey === id || s.label === id || s.sourceLabel === id);
      return src ? (src.label || src.sourceLabel || id) : id;
    });
  }
  getModifiedText(q: any): string {
    return q.modified || relativeDate(q.updatedAt || new Date().toISOString());
  }
  openWizard() {
    this.editingQuery = undefined;
    this.showWizard = true;
  }
  edit(query: DataQuery) {
    this.editingQuery = query;
    this.showWizard = true;
  }
  duplicate(id: string) {
    this.queryService.duplicateQuery(id);
  }
  deleteQuery(id: string) {
    this.queryToDeleteId = id;
    this.deleteConfirmOpen = true;
  }
  confirmDeleteQuery() {
    if (this.queryToDeleteId) {
      this.queryService.deleteQuery(this.queryToDeleteId);
      this.queryToDeleteId = null;
    }
    this.deleteConfirmOpen = false;
  }
  saveQuery(query: DataQuery) {
    this.queryService.upsertQuery(query);
    this.showWizard = false;
  }
  openShare(query: DataQuery) {
    this.shareTarget = query;
    this.shareOpen = true;
  }
}
