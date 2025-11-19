// src/app/guards/role.guard.ts

import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

// ========================================
// 🔴 GUARD PARA ADMIN
// ========================================
export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isLoggedIn()) {
    router.navigate(['/login'], {
      queryParams: { returnUrl: state.url }
    });
    return false;
  }

  if (authService.isAdmin()) {
    return true;
  }

  console.warn('⚠️ Acceso denegado: Se requiere rol ADMIN');
  router.navigate(['/dashboard']);
  return false;
};

// ========================================
// 🟢 GUARD PARA CLIENTE
// ========================================
export const clienteGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isLoggedIn()) {
    router.navigate(['/login'], {
      queryParams: { returnUrl: state.url }
    });
    return false;
  }

  if (authService.isCliente()) {
    return true;
  }

  console.warn('⚠️ Acceso denegado: Se requiere rol CLIENTE');
  router.navigate(['/dashboard']);
  return false;
};

// ========================================
// 🟡 GUARD PARA REPARTIDOR
// ========================================
export const repartidorGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isLoggedIn()) {
    router.navigate(['/login'], {
      queryParams: { returnUrl: state.url }
    });
    return false;
  }

  if (authService.isRepartidor()) {
    return true;
  }

  console.warn('⚠️ Acceso denegado: Se requiere rol REPARTIDOR');
  router.navigate(['/dashboard']);
  return false;
};