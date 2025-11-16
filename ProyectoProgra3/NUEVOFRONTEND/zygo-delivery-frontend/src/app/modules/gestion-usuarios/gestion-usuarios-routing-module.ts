// src/app/modules/gestion-usuarios/gestion-usuarios-routing-module.ts

import { Routes } from '@angular/router';

export const USUARIOS_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'listar',
    pathMatch: 'full'
  },
  {
    path: 'listar',
    loadComponent: () => import('./listar-usuarios/listar-usuarios').then(m => m.ListarUsuariosComponent)
  },
  {
    path: 'crear',
    loadComponent: () => import('./crear-usuarios/crear-usuarios').then(m => m.CrearUsuariosComponent)
  },
  {
    path: 'editar/:id',
    loadComponent: () => import('./editar-usuarios/editar-usuarios').then(m => m.EditarUsuariosComponent)
  }
];