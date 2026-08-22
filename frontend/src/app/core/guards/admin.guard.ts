import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { AppRole } from '@core/enums/app-role.enum';

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router      = inject(Router);
  if (authService.hasRole(AppRole.TENANT_ADMIN) || authService.hasRole(AppRole.SUPER_ADMIN)) {
    return true;
  }
  router.navigate(['/']);
  return false;
};
