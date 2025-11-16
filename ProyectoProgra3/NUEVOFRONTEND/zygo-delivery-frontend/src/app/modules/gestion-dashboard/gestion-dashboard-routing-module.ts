// src/app/modules/gestion-dashboard/gestion-dashboard-routing-module.ts

import { Routes } from '@angular/router';

export const DASHBOARD_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./home/home').then(m => m.HomeComponent)
  }
];