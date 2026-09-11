import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserAccountDto, CreateUserRequest } from '@core/api/dtos/user.dto';

const API_URL = (typeof window !== 'undefined' && window.location.hostname === 'localhost' && window.location.port === '4200')
  ? 'http://localhost:8080/api'
  : '/api';
const TOKEN_KEY = 'cockpit_jwt';

@Injectable({ providedIn: 'root' })
export class IdentityService {

  constructor(private http: HttpClient) {}

  getAllUsers(): Observable<UserAccountDto[]> {
    return this.http.get<UserAccountDto[]>(`${API_URL}/identity/users`);
  }

  getUserById(id: string): Observable<UserAccountDto> {
    return this.http.get<UserAccountDto>(`${API_URL}/identity/${id}`);
  }

  /**
   * Creates a new user account (STANDALONE mode, admin only).
   * The backend sends the activation email automatically.
   * tenantId is extracted silently from the caller's JWT — never entered in the form.
   * SYSTEM_ADMIN: uses their own tenantId from the JWT for now.
   *               When tenant management is built, a tenant selector will be added.
   */
  createUser(username: string, email: string, displayName: string): Observable<UserAccountDto> {
    const tenantId = this.extractTenantId();
    const body: CreateUserRequest = { username, email, displayName, tenantId: tenantId ?? '' };
    return this.http.post<UserAccountDto>(`${API_URL}/identity/users`, body);
  }

  deleteUser(id: string): Observable<void> {
    return this.http.delete<void>(`${API_URL}/identity/users/${id}`);
  }

  private extractTenantId(): string | null {
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token) return null;
    try {
      return JSON.parse(atob(token.split('.')[1])).tenantId ?? null;
    } catch {
      return null;
    }
  }
}
