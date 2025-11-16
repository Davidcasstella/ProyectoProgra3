// src/app/modules/gestion-mapas/gestion-mapas-routing-module.ts

import { Routes } from '@angular/router';
import { SelectorUbicacionComponent } from './selector-ubicacion/selector-ubicacion';

export const MAPAS_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'selector-ubicacion',
    pathMatch: 'full'
  },
  {
    path: 'selector-ubicacion',
    component: SelectorUbicacionComponent
  }
];