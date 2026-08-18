import { DashboardResponseDto, DashboardRequestDto } from '@core/api/dtos/dashboard.dto';
import { DashboardMapper } from '@core/api/mappers/dashboard.mapper';
import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, map } from 'rxjs';
import { Dashboard } from '@core/models/types';
import { uid } from '@core/utils/utils';
import { AuditService } from '@pages/settings/services/audit.service';
import { SseService } from '@core/services/sse.service';

const nowIso = () => new Date().toISOString();
const API_URL = '/api';
function getStoredDashboards(): Dashboard[] {
  if (typeof localStorage !== 'undefined') {
    const raw = localStorage.getItem('cockpit_dashboards');
    if (raw) {
      try {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed)) return parsed;
      } catch {}
    }
  }
  return [];
}
function saveStoredDashboards(list: Dashboard[]): void {
  if (typeof localStorage !== 'undefined' && Array.isArray(list)) {
    localStorage.setItem('cockpit_dashboards', JSON.stringify(list));
  }
}
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private dashboardsSubject = new BehaviorSubject<Dashboard[]>(getStoredDashboards());
  dashboards$: Observable<Dashboard[]> = this.dashboardsSubject.asObservable();
  
  constructor(private http: HttpClient, private ngZone: NgZone, private auditService: AuditService, private sseService: SseService) {
    this.loadFromBackend();
    this.sseService.events$.subscribe(event => {
      if (event.channel === 'queries_changed' || event.channel === 'dashboards_changed') {
        this.loadFromBackend();
      }
    });
  }

  private setDashboards(list: Dashboard[]) {
    saveStoredDashboards(list);
    this.ngZone.run(() => this.dashboardsSubject.next(list));
  }
  public loadFromBackend(): void {
    this.http.get<DashboardResponseDto[]>(`${API_URL}/dashboards`).subscribe({
      next: (data: any) => {
        if (Array.isArray(data)) {
          const cleanModels = data.map((dto: DashboardResponseDto) => DashboardMapper.toDomain(dto));
          this.setDashboards(cleanModels);
        }
      },
      error: () => {
        this.setDashboards(this.dashboards);
      }
    });
  }
  get dashboards(): Dashboard[] {
    return this.dashboardsSubject.value;
  }
  getDashboard(id: string): Dashboard | undefined {
    return this.dashboards.find((d) => d.id === id);
  }
  upsertDashboard(dashboard: Dashboard): void {
    const prev = this.dashboards;
    const exists = prev.some((d) => d.id === dashboard.id);
    const next: Dashboard = {
      ...dashboard,
      updatedAt: nowIso()
    };
    const updated = exists
      ? prev.map((d) => (d.id === dashboard.id ? next : d))
      : [next, ...prev];
    this.setDashboards(updated);
    if (exists) {
      const dto = DashboardMapper.toDto(dashboard);
      this.http.put<DashboardResponseDto>(`${API_URL}/dashboards/${dashboard.id}`, dto).subscribe({
        next: () => {
          this.loadFromBackend();
          this.auditService.loadAuditLogs();
        }
      });
    } else {
      const dto = DashboardMapper.toDto(dashboard);
      this.http.post<DashboardResponseDto>(`${API_URL}/dashboards`, dto).subscribe({
        next: (createdDto: DashboardResponseDto) => {
          const created = DashboardMapper.toDomain(createdDto);
          if (created && created.id) {
            const current = this.dashboards.filter((d) => d.id !== dashboard.id && d.id !== created.id);
            this.setDashboards([created, ...current]);
          }
          this.loadFromBackend();
          this.auditService.loadAuditLogs();
        }
      });
    }
  }
  deleteDashboard(id: string): void {
    this.dashboardsSubject.next(this.dashboards.filter((d) => d.id !== id));
    this.http.delete(`${API_URL}/dashboards/${id}`).subscribe({
      next: () => this.auditService.loadAuditLogs()
    });
  }
  duplicateDashboard(id: string): Dashboard | undefined {
    const source = this.dashboards.find((d) => d.id === id);
    if (!source) return undefined;
    const copyId = uid('d');
    const existingNames = this.dashboards.map((d) => d.name);
    const cleanBase = source.name.replace(/(\s*\(copie\s*\d*\))+$/gi, '').trim();
    let counter = 1;
    let newName = `${cleanBase} (copie ${counter})`;
    while (existingNames.some((n) => n.toLowerCase() === newName.toLowerCase())) {
      counter++;
      newName = `${cleanBase} (copie ${counter})`;
    }
    const copy: Dashboard = {
      ...source,
      id: copyId,
      name: newName,
      status: 'draft',
      favorite: false,
      owner: 'Vous',
      sharedWithMe: false,
      createdAt: nowIso(),
      updatedAt: nowIso(),
      widgets: source.widgets.map((wd) => ({
        ...wd,
        id: uid('w'),
        filters: wd.filters?.map((f) => ({ ...f, id: uid('wf') }))
      }))
    };
    this.dashboardsSubject.next([copy, ...this.dashboards]);
    this.http.post<DashboardResponseDto>(`${API_URL}/dashboards/${id}/duplicate`, {}).subscribe({
      next: (createdDto: DashboardResponseDto) => {
        const created = DashboardMapper.toDomain(createdDto);
        if (created && created.id) {
          created.name = newName;
          const current = this.dashboards;
          const idx = current.findIndex(d => d.id === copyId);
          if (idx !== -1) {
            current[idx] = created;
            this.dashboardsSubject.next([...current]);
          }
          const createdDtoForPut = DashboardMapper.toDto(created);
          this.http.put(`${API_URL}/dashboards/${created.id}`, createdDtoForPut).subscribe({
            next: () => this.auditService.loadAuditLogs()
          });
        }
      }
    });
    return copy;
  }
  toggleFavorite(id: string): void {
    this.dashboardsSubject.next(
      this.dashboards.map((d) =>
        d.id === id
          ? {
              ...d,
              favorite: !d.favorite
            }
          : d
      )
    );
    this.http.patch(`${API_URL}/dashboards/${id}/favorite`, {}).subscribe({
      next: () => this.auditService.loadAuditLogs()
    });
  }
  archiveDashboard(id: string): void {
    this.dashboardsSubject.next(
      this.dashboards.map((d) =>
        d.id === id
          ? {
              ...d,
              archived: !d.archived
            }
          : d
      )
    );
    this.http.patch(`${API_URL}/dashboards/${id}/archive`, {}).subscribe({
      next: () => this.auditService.loadAuditLogs()
    });
  }
  createDashboard(partial: Partial<Dashboard>): Observable<Dashboard> {
    const tempId = uid('d');
    const dashboard: Dashboard = {
      id: tempId,
      name: 'Nouveau tableau de bord',
      description: '',
      color: '#2563eb',
      status: 'draft',
      owner: 'Vous',
      shareLevel: 'private',
      columns: 12,
      density: 'compact',
      refreshInterval: 'off',
      widgets: [],
      globalFilters: [],
      tags: [],
      favorite: false,
      archived: false,
      createdAt: nowIso(),
      updatedAt: nowIso(),
      ...partial
    };
    const dto = DashboardMapper.toDto(dashboard);
    return this.http.post<DashboardResponseDto>(`${API_URL}/dashboards`, dto).pipe(
      map((createdDto: DashboardResponseDto) => {
        const created = DashboardMapper.toDomain(createdDto) || dashboard;
        const current = this.dashboards;
        this.setDashboards([created, ...current.filter((d) => d.id !== tempId && d.id !== created.id)]);
        this.auditService.loadAuditLogs();
        return created;
      })
    );
  }
}
