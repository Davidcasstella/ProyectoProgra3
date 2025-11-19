// src/app/modules/gestion-dashboard/gestion-dashboard-routing-module.ts

import { Routes } from '@angular/router';
import { HomeComponent } from './home/home';
import { GuardarUbicacionClienteComponent } from './guardar-ubicacion-cliente/guardar-ubicacion-cliente';

export const DASHBOARD_ROUTES: Routes = [
  {
    path: '',
    component: HomeComponent
  },
  {
    path: 'guardar-ubicacion',
    component: GuardarUbicacionClienteComponent
  }
];