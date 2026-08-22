import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  GroupDto,
  CreateGroupRequest,
  GroupMemberDto,
  UserGroupDto
} from '@core/api/dtos/user.dto';

const API = 'http://localhost:8080/api';

@Injectable({ providedIn: 'root' })
export class GroupService {

  constructor(private http: HttpClient) {}

  /** Returns all groups. */
  getGroups(): Observable<GroupDto[]> {
    return this.http.get<GroupDto[]>(`${API}/identity/groups`);
  }

  /** Creates a new group. STANDALONE mode only. */
  createGroup(req: CreateGroupRequest): Observable<GroupDto> {
    return this.http.post<GroupDto>(`${API}/identity/groups`, req);
  }

  /**
   * Returns the groups a user belongs to (user-centric view).
   * Distinct from getGroupMembers() which is group-centric.
   */
  getUserGroups(userId: string): Observable<UserGroupDto[]> {
    return this.http.get<UserGroupDto[]>(`${API}/identity/users/${userId}/groups`);
  }

  /** Returns the members of a group (group-centric view). */
  getGroupMembers(groupId: string): Observable<GroupMemberDto[]> {
    return this.http.get<GroupMemberDto[]>(`${API}/identity/groups/${groupId}/members`);
  }

  /** Adds a user to a group with the given membership role. STANDALONE mode only. */
  addToGroup(groupId: string, userId: string, membershipRole = 'MEMBER'): Observable<GroupMemberDto> {
    return this.http.post<GroupMemberDto>(
      `${API}/identity/groups/${groupId}/members`,
      { userId, membershipRole }
    );
  }

  /** Removes a user from a group. STANDALONE mode only. */
  removeFromGroup(groupId: string, userId: string): Observable<void> {
    return this.http.delete<void>(`${API}/identity/groups/${groupId}/members/${userId}`);
  }

  /** Permanently deletes a group and all its memberships/share grants. STANDALONE + admin only. */
  deleteGroup(groupId: string): Observable<void> {
    return this.http.delete<void>(`${API}/identity/groups/${groupId}`);
  }
}
