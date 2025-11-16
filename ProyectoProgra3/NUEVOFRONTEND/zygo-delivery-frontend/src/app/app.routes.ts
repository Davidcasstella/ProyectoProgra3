// src/app/app.routes.ts

import { Routes } from '@angular/router';
import { authGuard } from './guards/auth-guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () => import('./modules/autenticacion/login/login').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./modules/autenticacion/register/register').then(m => m.RegisterComponent)
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./modules/gestion-dashboard/home/home').then(m => m.HomeComponent)
  },
  {
    path: 'usuarios',
    canActivate: [authGuard],
    loadComponent: () => import('./modules/gestion-usuarios/listar-usuarios/listar-usuarios').then(m => m.ListarUsuariosComponent)
  },
  {
    path: 'usuarios/crear',
    canActivate: [authGuard],
    loadComponent: () => import('./modules/gestion-usuarios/crear-usuarios/crear-usuarios').then(m => m.CrearUsuariosComponent)
  },
  {
    path: 'usuarios/editar/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./modules/gestion-usuarios/editar-usuarios/editar-usuarios').then(m => m.EditarUsuariosComponent)
  },
  // ✨ PEDIDOS - Ahora con Standalone Components
  {
    path: 'pedidos',
    canActivate: [authGuard],
    loadChildren: () => import('./modules/gestion-pedidos/gestion-pedidos-routing-module')
      .then(m => m.PEDIDOS_ROUTES)
  },
  // ✨ MAPAS
  {
    path: 'mapas',
    canActivate: [authGuard],
    loadChildren: () => import('./modules/gestion-mapas/gestion-mapas-routing-module')
      .then(m => m.MAPAS_ROUTES)
  },
  {
    path: '**',
    redirectTo: '/login'
  }
];