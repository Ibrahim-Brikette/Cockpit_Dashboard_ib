import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RoleDto, UserRoleDto } from '@core/api/dtos/user.dto';

const API = 'http://localhost:8080/api';

@Injectable({ providedIn: 'root' })
export class RoleService {

  constructor(private http: HttpClient) {}

  /** Returns the full list of system roles (seeded at startup). */
  getRoles(): Observable<RoleDto[]> {
    return this.http.get<RoleDto[]>(`${API}/identity/roles`);
  }

  /** Returns the roles assigned to a specific user. */
  getUserRoles(userId: string): Observable<UserRoleDto[]> {
    return this.http.get<UserRoleDto[]>(`${API}/identity/users/${userId}/roles`);
  }

  /** Assigns a role to a user. STANDALONE mode only — returns 404 in INTEGRATED. */
  assignRole(userId: string, roleId: string, assignmentScope = 'ORGANIZATION'): Observable<UserRoleDto> {
    return this.http.post<UserRoleDto>(
      `${API}/identity/users/${userId}/roles`,
      { roleId, assignmentScope }
    );
  }

  /** Removes a role from a user. STANDALONE mode only. */
  removeRole(userId: string, roleId: string): Observable<void> {
    return this.http.delete<void>(`${API}/identity/users/${userId}/roles/${roleId}`);
  }
}
