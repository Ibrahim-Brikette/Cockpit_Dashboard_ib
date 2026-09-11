import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ShareGrantDto, CreateShareGrantRequest } from '@core/api/dtos/sharing.dto';

const API = (typeof window !== 'undefined' && window.location.hostname === 'localhost' && window.location.port === '4200')
  ? 'http://localhost:8080/api'
  : '/api';

@Injectable({ providedIn: 'root' })
export class ShareGrantService {

  constructor(private http: HttpClient) {}

  // ── Dashboard grants ─────────────────────────────────────────────────────

  getDashboardGrants(dashboardId: string): Observable<ShareGrantDto[]> {
    return this.http.get<ShareGrantDto[]>(`${API}/sharing/dashboards/${dashboardId}/grants`);
  }

  createDashboardGrant(dashboardId: string, req: CreateShareGrantRequest): Observable<ShareGrantDto> {
    return this.http.post<ShareGrantDto>(`${API}/sharing/dashboards/${dashboardId}/grants`, req);
  }

  removeDashboardGrant(dashboardId: string, grantId: string): Observable<void> {
    return this.http.delete<void>(`${API}/sharing/dashboards/${dashboardId}/grants/${grantId}`);
  }

  // ── Query grants ─────────────────────────────────────────────────────────

  getQueryGrants(queryId: string): Observable<ShareGrantDto[]> {
    return this.http.get<ShareGrantDto[]>(`${API}/sharing/queries/${queryId}/grants`);
  }

  createQueryGrant(queryId: string, req: CreateShareGrantRequest): Observable<ShareGrantDto> {
    return this.http.post<ShareGrantDto>(`${API}/sharing/queries/${queryId}/grants`, req);
  }

  removeQueryGrant(queryId: string, grantId: string): Observable<void> {
    return this.http.delete<void>(`${API}/sharing/queries/${queryId}/grants/${grantId}`);
  }
}
