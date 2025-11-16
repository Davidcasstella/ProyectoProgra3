// src/app/modules/gestion-dashboard/home/home.ts

import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router'; // ✅ IMPORTANTE: Importar RouterModule
import { NavbarComponent } from '../../../components/navbar/navbar';
import { PedidoService } from '../../../services/pedido.service';
import { UsuarioService } from '../../../services/usuario.service';
import { AuthService } from '../../../services/auth.service';
import { Pedido } from '../../../models/pedido.model';
import { Usuario, UsuarioDTO } from '../../../models/usuario.model';

interface StatCard {
  title: string;
  label: string;
  value: string;
  change: string;
  icon: string;
  svg: string;
  color: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule,      // ✅ IMPORTANTE: Agregar RouterModule aquí
    NavbarComponent
  ],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class HomeComponent implements OnInit {
  private pedidoService = inject(PedidoService);
  private usuarioService = inject(UsuarioService);
  private authService = inject(AuthService);

  usuarioActual = this.authService.currentUserSignal;

  pedidos = signal<Pedido[]>([]);
  usuarios = signal<UsuarioDTO[]>([]);
  loading = signal(true);

  stats = computed<StatCard[]>(() => {
    const totalPedidos = this.pedidos().length;
    const pedidosPendientes = this.pedidos().filter(p => p.estado === 'PENDIENTE').length;
    const pedidosEntregados = this.pedidos().filter(p => p.estado === 'ENTREGADO').length;
    const totalUsuarios = this.usuarios().length;

    return [
      {
        title: 'Total Pedidos',
        label: 'Total Pedidos',
        value: totalPedidos.toString(),
        change: '+12% vs mes anterior',
        icon: 'M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z M3 6h18 M16 10a4 4 0 0 1-8 0',
        svg: '<path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 0 1-8 0"/>',
        color: '#8b5cf6'
      },
      {
        title: 'Pedidos Pendientes',
        label: 'Pedidos Pendientes',
        value: pedidosPendientes.toString(),
        change: 'Requieren asignación',
        icon: 'M12 8v4l3 3m6-3a9 9 0 1 1-18 0 9 9 0 0 1 18 0z',
        svg: '<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>',
        color: '#f59e0b'
      },
      {
        title: 'Entregados',
        label: 'Entregados',
        value: pedidosEntregados.toString(),
        change: '+8% esta semana',
        icon: 'M9 12l2 2 4-4m6 2a9 9 0 1 1-18 0 9 9 0 0 1 18 0z',
        svg: '<path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>',
        color: '#10b981'
      },
      {
        title: 'Usuarios Activos',
        label: 'Usuarios Activos',
        value: totalUsuarios.toString(),
        change: '+3 nuevos hoy',
        icon: 'M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2 M23 21v-2a4 4 0 0 0-3-3.87 M9 7a4 4 0 1 0 0-8 4 4 0 0 0 0 8z M16 3.13a4 4 0 0 1 0 7.75',
        svg: '<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
        color: '#3b82f6'
      }
    ];
  });

  recentPedidos = computed(() => {
    return this.pedidos()
      .sort((a, b) => {
        const dateA = a.fechaCreacion ? new Date(a.fechaCreacion).getTime() : 0;
        const dateB = b.fechaCreacion ? new Date(b.fechaCreacion).getTime() : 0;
        return dateB - dateA;
      })
      .slice(0, 5);
  });

  // Alias para compatibilidad con el HTML
  estadisticas = this.stats;
  pedidosRecientes = this.recentPedidos;

  // Computed para pedidos por estado (para gráficos)
  pedidosPorEstado = computed(() => {
    const pedidos = this.pedidos();
    const estados = ['PENDIENTE', 'ASIGNADO', 'EN_CAMINO', 'ENTREGADO', 'CANCELADO'];
    
    return estados.map(estado => ({
      label: estado,
      value: pedidos.filter(p => p.estado === estado).length,
      color: this.getColorForEstado(estado)
    }));
  });

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading.set(true);

    this.pedidoService.obtenerTodos().subscribe({
      next: (data: Pedido[]) => {
        this.pedidos.set(data);
      },
      error: (error: any) => {
        console.error('Error al cargar pedidos:', error);
      }
    });

    this.usuarioService.obtenerTodos().subscribe({
      next: (data: UsuarioDTO[]) => {
        this.usuarios.set(data);
        this.loading.set(false);
      },
      error: (error: any) => {
        console.error('Error al cargar usuarios:', error);
        this.loading.set(false);
      }
    });
  }

  getColorForEstado(estado: string): string {
    const colores: { [key: string]: string } = {
      'PENDIENTE': '#f59e0b',
      'ASIGNADO': '#3b82f6',
      'EN_CAMINO': '#8b5cf6',
      'ENTREGADO': '#10b981',
      'CANCELADO': '#ef4444'
    };
    return colores[estado] || '#6b7280';
  }

  getBarHeight(value: number): number {
    const max = Math.max(...this.pedidosPorEstado().map(d => d.value), 1);
    return (value / max) * 100;
  }

  getEstadoClass(estado: string | undefined): string {
    if (!estado) return 'status-badge';
    const classes: { [key: string]: string } = {
      'PENDIENTE': 'status-badge status-pending',
      'ASIGNADO': 'status-badge status-assigned',
      'EN_CAMINO': 'status-badge status-in-transit',
      'ENTREGADO': 'status-badge status-delivered',
      'CANCELADO': 'status-badge status-cancelled'
    };
    return classes[estado] || 'status-badge';
  }

  formatearFecha(fecha: Date | string | undefined): string {
    if (!fecha) return 'N/A';
    const date = new Date(fecha);
    return date.toLocaleDateString('es-CO', {
      day: '2-digit',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  // Alias para compatibilidad con HTML
  formatFecha = this.formatearFecha;

  formatearPrecio(precio: number | undefined): string {
    if (!precio) return '$0';
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      minimumFractionDigits: 0
    }).format(precio);
  }
}