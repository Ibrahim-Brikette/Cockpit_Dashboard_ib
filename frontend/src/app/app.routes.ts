import { Routes } from '@angular/router';
import { authGuard } from '@core/guards/auth.guard';
export const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('@pages/dashboard/dashboard.routes').then(m => m.routes),
    canActivate: [authGuard]
  },
  {
    path: 'requetes',
    loadChildren: () => import('@pages/query/query.routes').then(m => m.routes),
    canActivate: [authGuard]
  },
  {
    path: 'parametres',
    loadChildren: () => import('@pages/settings/settings.routes').then(m => m.routes),
    canActivate: [authGuard]
  },
  {
    path: 'sources-de-donnees',
    loadChildren: () => import('@pages/data-sources/data-sources.routes').then(m => m.routes),
    canActivate: [authGuard]
  },
  {
    path: 'admin-kpi',
    loadChildren: () => import('@pages/admin-kpi/admin-kpi.routes').then(m => m.routes),
    canActivate: [authGuard]
  },
  {
    path: 'alertes',
    loadChildren: () => import('@pages/alerts/alerts.routes').then(m => m.routes),
    canActivate: [authGuard]
  },
  { path: '**', redirectTo: '' }
];
