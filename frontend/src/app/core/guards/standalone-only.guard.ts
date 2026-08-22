import { Injectable } from '@angular/core';
import { Router, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { filter, first, switchMap, map } from 'rxjs/operators';
import { AuthService } from '@core/services/auth.service';

/**
 * Blocks STANDALONE-only routes (login, forgot-password, reset-password, verify-email)
 * when the backend runs in INTEGRATED mode.
 *
 * Equivalent of @ConditionalOnProperty(havingValue = "STANDALONE") on the backend.
 * Waits for the /api/config call to resolve before deciding — prevents the default
 * 'STANDALONE' value from incorrectly allowing access before the real mode is known.
 */
@Injectable({ providedIn: 'root' })
export class standaloneOnlyGuard {

  constructor(private authService: AuthService, private router: Router) {}

  canActivate(): Observable<boolean | UrlTree> {
    return this.authService.modeReady$.pipe(
      filter(ready => ready),
      first(),
      switchMap(() => this.authService.mode$.pipe(first())),
      map(mode => {
        if (mode === 'INTEGRATED') {
          return this.router.createUrlTree(['/']);
        }
        return true;
      })
    );
  }
}
