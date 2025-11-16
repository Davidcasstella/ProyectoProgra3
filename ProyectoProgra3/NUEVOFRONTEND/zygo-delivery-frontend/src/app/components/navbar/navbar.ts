// src/app/components/navbar/navbar.ts

import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

interface MenuItem {
  label: string;
  route?: string;
  svg: string;
  children?: MenuItem[];
  expanded?: boolean;
}

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.css']
})
export class NavbarComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  menuExpanded = signal(true);
  
  // Usar el signal del AuthService
  usuarioActual = this.authService.currentUserSignal;

  menuItems: MenuItem[] = [
    {
      label: 'Dashboard',
      route: '/dashboard',
      svg: '<path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>'
    },
    {
      label: 'Usuarios',
      route: '/usuarios',
      svg: '<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>'
    },
    {
      label: 'Pedidos',
      svg: '<path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 0 1-8 0"/>',
      expanded: false,
      children: [
        {
          label: 'Listar Pedidos',
          route: '/pedidos/listar-pedidos',
          svg: '<rect x="3" y="3" width="18" height="18" rx="2"/><path d="M9 9h6M9 13h6"/>'
        },
        {
          label: 'Crear Pedido',
          route: '/pedidos/crear-pedidos',
          svg: '<circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/>'
        }
      ]
    },
    {
      label: 'Mapa',
      route: '/mapas/selector-ubicacion',
      svg: '<circle cx="12" cy="10" r="3"/><path d="M12 21.7C17.3 17 20 13 20 10a8 8 0 1 0-16 0c0 3 2.7 6.9 8 11.7z"/>'
    }
  ];

  toggleMenu(): void {
    this.menuExpanded.update(v => !v);
  }

  toggleSubmenu(item: MenuItem): void {
    if (item.children) {
      item.expanded = !item.expanded;
    }
  }

  getInitials(nombre: string): string {
    if (!nombre) return '?';
    const parts = nombre.split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return nombre.substring(0, 2).toUpperCase();
  }

  logout(): void {
    this.authService.logout();
  }
}