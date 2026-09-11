import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ShareGrantDto {
  id: string;
  shareLevel: string;
  accessLevel: string;
  granteeUserId?: string;
  granteeGroupId?: string;
  granteeName: string;
}

export interface CreateShareGrantRequest {
  shareLevel: string;
  accessLevel: string;
  granteeUserId?: string;
  granteeGroupId?: string;
}

const API_URL = (typeof window !== 'undefined' && window.location.hostname === 'localhost' && window.location.port === '4200')
  ? 'http://localhost:8080/api'
  : '/api';

@Injectable({ providedIn: 'root' })
export class SharingService {

  constructor(private http: HttpClient) {}

  getDashboardGrants(dashboardId: string): Observable<ShareGrantDto[]> {
    return this.http.get<ShareGrantDto[]>(`${API_URL}/sharing/dashboards/${dashboardId}/grants`);
  }

  addDashboardGrant(dashboardId: string, req: CreateShareGrantRequest): Observable<ShareGrantDto> {
    return this.http.post<ShareGrantDto>(`${API_URL}/sharing/dashboards/${dashboardId}/grants`, req);
  }

  removeDashboardGrant(dashboardId: string, grantId: string): Observable<void> {
    return this.http.delete<void>(`${API_URL}/sharing/dashboards/${dashboardId}/grants/${grantId}`);
  }

  getQueryGrants(queryId: string): Observable<ShareGrantDto[]> {
    return this.http.get<ShareGrantDto[]>(`${API_URL}/sharing/queries/${queryId}/grants`);
  }

  addQueryGrant(queryId: string, req: CreateShareGrantRequest): Observable<ShareGrantDto> {
    return this.http.post<ShareGrantDto>(`${API_URL}/sharing/queries/${queryId}/grants`, req);
  }

  removeQueryGrant(queryId: string, grantId: string): Observable<void> {
    return this.http.delete<void>(`${API_URL}/sharing/queries/${queryId}/grants/${grantId}`);
  }
}
