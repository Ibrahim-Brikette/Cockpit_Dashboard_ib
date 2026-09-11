import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, map, tap, finalize } from 'rxjs';
import { AppRole } from '@core/enums/app-role.enum';
import { UserProfile } from '@core/services/user.service';

const API_URL = (typeof window !== 'undefined' && window.location.hostname === 'localhost' && window.location.port === '4200')
  ? 'http://localhost:8080/api'
  : '/api';
const TOKEN_KEY = 'cockpit_jwt';

interface LoginResponse {
  access_token: string;
  token_type: string;
  expires_in: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {

  private currentUserSubject = new BehaviorSubject<UserProfile | null>(null);
  private rolesSubject      = new BehaviorSubject<string[]>([]);
  private modeSubject       = new BehaviorSubject<string>('STANDALONE');
  private modeReadySubject  = new BehaviorSubject<boolean>(false);
  private serverDownSubject = new BehaviorSubject<boolean>(false);

  currentUser$ = this.currentUserSubject.asObservable();
  mode$        = this.modeSubject.asObservable();
  modeReady$   = this.modeReadySubject.asObservable();
  serverDown$  = this.serverDownSubject.asObservable();

  get currentMode(): string { return this.modeSubject.getValue(); }
  get currentUser(): UserProfile | null { return this.currentUserSubject.getValue(); }

  constructor(private http: HttpClient, private router: Router) {
    this.loadMode();
    if (this.isAuthenticated()) {
      this.loadUserProfile();
    }
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  isAuthenticated(): boolean {
    return !!localStorage.getItem(TOKEN_KEY);
  }

  /**
   * Role check — works in both modes after loadUserProfile() has run.
   * Pass AppRole enum values for type safety: hasRole(AppRole.SUPER_ADMIN).
   * Accepts string for backwards compatibility with any remaining string literals.
   */
  hasRole(role: AppRole | string): boolean {
    return this.rolesSubject.value.includes(role as string);
  }

  /**
   * Permission check — only meaningful in INTEGRATED mode where the JWT carries
   * a "roles" claim. In STANDALONE mode permissions are resolved server-side via
   * @PreAuthorize; the JWT carries no permission codes.
   * Use hasRole() for UI show/hide in STANDALONE mode.
   */
  hasPermission(_code: string): boolean {
    return false;
  }

  // ── Auth flows ──────────────────────────────────────────────────────────────

  login(username: string, password: string): Observable<void> {
    return this.http.post<LoginResponse>(`${API_URL}/auth/login`, { username, password }, { withCredentials: true }).pipe(
      tap(response => {
        localStorage.setItem(TOKEN_KEY, response.access_token);
        this.loadUserProfile();
      }),
      map(() => undefined as void)
    );
  }

  /**
   * Called by the interceptor on 401.
   * The browser automatically sends the HttpOnly refresh_token cookie.
   * Returns the new access token so the interceptor can retry the original request.
   */
  refresh(): Observable<string> {
    return this.http.post<LoginResponse>(
      `${API_URL}/auth/refresh`, {}, { withCredentials: true }
    ).pipe(
      tap(response => localStorage.setItem(TOKEN_KEY, response.access_token)),
      map(response => response.access_token)
    );
  }

  logout(): void {
    // Fire the server-side logout first (token still in localStorage so interceptor
    // attaches it). The local cleanup and redirect happen immediately regardless.
    this.http.post(`${API_URL}/auth/logout`, {}).subscribe({ error: () => {} });
    localStorage.removeItem(TOKEN_KEY);
    this.currentUserSubject.next(null);
    this.rolesSubject.next([]);
    // INTEGRATED mode has no login page — go to root and let authGuard block silently.
    this.router.navigate([this.currentMode === 'INTEGRATED' ? '/' : '/auth/connexion']);
  }

  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>(`${API_URL}/auth/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${API_URL}/auth/password-reset`, { token, newPassword });
  }

  verifyEmail(token: string, password: string): Observable<void> {
    return this.http.post<void>(`${API_URL}/auth/verify-email`, { token, password });
  }

  // ── Profile loading ─────────────────────────────────────────────────────────

  loadUserProfile(): void {
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token) return;

    // Roles are in the JWT in both STANDALONE and INTEGRATED modes.
    // Extract immediately — no HTTP call needed for this.
    this.rolesSubject.next(this.extractRoles(token));

    const userId = this.extractSub(token);
    if (!userId) return;

    // HTTP call only for displayName, username, email — not present in the JWT.
    this.http.get<any>(`${API_URL}/identity/${userId}`).subscribe({
      next: user => {
        this.currentUserSubject.next({
          id:          user.id,
          username:    user.username,
          email:       user.email,
          displayName: user.displayName,
          initials:    this.buildInitials(user.displayName || user.username)
        });
      },
      error: (err: HttpErrorResponse) => {
        // Status 0 = server not responding (restart window, network down).
        // The JWT is still valid — the server just isn't up yet.
        // Don't logout. The user can reload once the server is back.
        if (err.status === 0) return;
        // INTEGRATED mode — token is managed externally, never auto-logout.
        if (this.currentMode === 'INTEGRATED') return;
        if (!this.currentUserSubject.value) {
          this.logout();
        }
      }
    });
  }

  // ── Private helpers ─────────────────────────────────────────────────────────

  private loadMode(): void {
    this.http.get<{ mode: string }>(`${API_URL}/config`).subscribe({
      next: res => {
        this.serverDownSubject.next(false);
        this.modeSubject.next(res.mode);
        this.modeReadySubject.next(true);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 0) {
          // Server is not yet up (restart window). Show the restart overlay
          // and keep retrying every 5 seconds until it responds.
          this.serverDownSubject.next(true);
          setTimeout(() => this.loadMode(), 5_000);
        } else {
          this.modeSubject.next('STANDALONE');
          this.modeReadySubject.next(true);
        }
      }
    });
  }

  private extractSub(token: string): string | null {
    try {
      return JSON.parse(atob(token.split('.')[1])).sub ?? null;
    } catch {
      return null;
    }
  }

  private extractRoles(token: string): string[] {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return Array.isArray(payload.roles) ? payload.roles : [];
    } catch {
      return [];
    }
  }

  private buildInitials(name: string): string {
    return name.split(' ').map((n: string) => n[0]).join('').toUpperCase().slice(0, 2);
  }
}
