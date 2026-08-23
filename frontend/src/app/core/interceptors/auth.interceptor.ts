import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '@core/services/auth.service';

/** URLs that must never trigger the 401→refresh→retry cycle. */
const AUTH_URLS = ['/api/auth/', '/api/config'];

export const authInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  // Skip auth endpoints — they are public or handle token rotation themselves.
  if (AUTH_URLS.some(url => req.url.includes(url))) {
    return next(req);
  }

  const token = localStorage.getItem('cockpit_jwt');
  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401) {
        return throwError(() => error);
      }

      const authService = inject(AuthService);

      // INTEGRATED mode — no refresh endpoint exists, just propagate the 401.
      // Never call logout() — the token is managed externally (mother app).
      if (authService.currentMode === 'INTEGRATED') {
        return throwError(() => error);
      }

      // STANDALONE — attempt a silent token refresh then retry the original request once.
      return authService.refresh().pipe(
        switchMap(newToken => {
          const retryReq = req.clone({ setHeaders: { Authorization: `Bearer ${newToken}` } });
          return next(retryReq);
        }),
        catchError(refreshError => {
          // Refresh also failed — session is dead, send user to login.
          authService.logout();
          return throwError(() => refreshError);
        })
      );
    })
  );
};
