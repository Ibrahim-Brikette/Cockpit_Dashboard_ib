import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { filter, first, map, switchMap } from 'rxjs/operators';
import { AuthService } from '@core/services/auth.service';

export const authGuard: CanActivateFn = () => {
  const router      = inject(Router);
  const authService = inject(AuthService);

  if (localStorage.getItem('cockpit_jwt')) {
    return true;
  }

  // Wait for the mode to be known before deciding where to redirect.
  // STANDALONE → redirect to login page.
  // INTEGRATED → block silently (no redirect — login page does not exist in this mode,
  //              redirecting there would create an infinite loop with standaloneOnlyGuard).
  return authService.modeReady$.pipe(
    filter(ready => ready),
    first(),
    switchMap(() => authService.mode$.pipe(first())),
    map(mode => {
      if (mode !== 'INTEGRATED') {
        router.navigate(['/auth/connexion']);
      }
      return false;
    })
  );
};
