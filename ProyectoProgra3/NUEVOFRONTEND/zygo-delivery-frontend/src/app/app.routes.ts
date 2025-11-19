// src/app/app.routes.ts

import { Routes } from '@angular/router';
import { authGuard } from './guards/auth-guard';
import { GuardarUbicacionClienteComponent } from './modules/gestion-dashboard/guardar-ubicacion-cliente/guardar-ubicacion-cliente';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  },
  // 🔐 AUTENTICACIÓN - Lazy Loading
  {
    path: '',
    loadChildren: () => import('./modules/autenticacion/autenticacion-routing-module')
      .then(m => m.AUTH_ROUTES)
  },
  // 🏠 DASHBOARD - Lazy Loading
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadChildren: () => import('./modules/gestion-dashboard/gestion-dashboard-routing-module')
      .then(m => m.DASHBOARD_ROUTES)
  },
  // 👥 USUARIOS - Lazy Loading
  {
    path: 'usuarios',
    canActivate: [authGuard],
    loadChildren: () => import('./modules/gestion-usuarios/gestion-usuarios-routing-module')
      .then(m => m.USUARIOS_ROUTES)
  },
  // 📦 PEDIDOS - Lazy Loading (ya estaba)
  {
    path: 'pedidos',
    canActivate: [authGuard],
    loadChildren: () => import('./modules/gestion-pedidos/gestion-pedidos-routing-module')
      .then(m => m.PEDIDOS_ROUTES)
  },
  // 🗺️ MAPAS - Lazy Loading (ya estaba)
  {
    path: 'mapas',
    canActivate: [authGuard],
    loadChildren: () => import('./modules/gestion-mapas/gestion-mapas-routing-module')
      .then(m => m.MAPAS_ROUTES)
  },
  // ❌ Ruta no encontrada

  {
    path: '**',
    redirectTo: '/login'
  }
];