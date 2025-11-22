// src/app/modules/gestion-pedidos/gestion-pedidos-routing-module.ts

import { Routes } from '@angular/router';

export const PEDIDOS_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'listar-pedidos',
    pathMatch: 'full'
  },
  {
    path: 'listar-pedidos',
    loadComponent: () => import('./listar-pedidos/listar-pedidos').then(m => m.ListarPedidos)
  },
  {
    path: 'crear-pedidos',
    loadComponent: () => import('./crear-pedidos/crear-pedidos').then(m => m.CrearPedidos)
  },
  {
    path: 'editar-pedidos/:id',
    loadComponent: () => import('./editar-pedidos/editar-pedidos').then(m => m.EditarPedidos)
  },
  // ✅ ESTA RUTA DEBE ESTAR AQUÍ
  {
    path: 'detalle-ruta/:id',
    loadComponent: () => import('./detalle-ruta/detalle-ruta').then(m => m.DetalleRuta)
  }
];