import { UserAccountDto } from '@core/api/dtos/user.dto';
import { UserMapper } from '@core/api/mappers/user.mapper';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
const API_URL = '/api';
export interface UserProfile {
  id?: string;
  username: string;
  email: string;
  displayName: string;
  initials?: string;
  accountStatus?: string;
  lastLoginAt?: string;
}
@Injectable({ providedIn: 'root' })
export class UserService {
  private currentUserSubject = new BehaviorSubject<UserProfile>({
    username: 'ahaddad',
    email: 'amine.haddad@prestacode.com',
    displayName: 'Amine Haddad',
    initials: 'AH'
  });
  private usersSubject = new BehaviorSubject<UserProfile[]>([]);
  currentUser$: Observable<UserProfile> = this.currentUserSubject.asObservable();
  users$: Observable<UserProfile[]> = this.usersSubject.asObservable();
  constructor(private http: HttpClient) {
    this.loadCurrentUser();
    this.loadUsers();
  }
  loadCurrentUser(): void {
    this.http.get<UserAccountDto>(`${API_URL}/identity/me`).subscribe({
      next: (user: UserAccountDto) => {
        if (user && user.displayName) {
          const mappedUser = UserMapper.toDomain(user);
          this.currentUserSubject.next(mappedUser);
        }
      },
      error: () => {}
    });
  }
  loadUsers(): void {
    this.http.get<UserAccountDto[]>(`${API_URL}/identity/users`).subscribe({
      next: (users: UserAccountDto[]) => {
        if (users && users.length > 0) {
          const mapped = users.map((u: UserAccountDto) => UserMapper.toDomain(u));
          this.usersSubject.next(mapped);
        }
      },
      error: () => {}
    });
  }
}
