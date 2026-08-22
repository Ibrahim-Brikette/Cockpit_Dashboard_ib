import { Routes } from '@angular/router';
import { noAuthGuard } from '@core/guards/noauth.guard';
import { standaloneOnlyGuard } from '@core/guards/standalone-only.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'connexion',
    pathMatch: 'full'
  },
  {
    path: 'connexion',
    canActivate: [standaloneOnlyGuard, noAuthGuard],
    loadComponent: () =>
      import('@pages/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'mot-de-passe-oublie',
    canActivate: [standaloneOnlyGuard, noAuthGuard],
    loadComponent: () =>
      import('@pages/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },
  {
    path: 'reinitialisation',
    canActivate: [standaloneOnlyGuard],
    loadComponent: () =>
      import('@pages/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent)
  },
  {
    path: 'verification-email',
    canActivate: [standaloneOnlyGuard],
    loadComponent: () =>
      import('@pages/auth/verify-email/verify-email.component').then(m => m.VerifyEmailComponent)
  }
];
