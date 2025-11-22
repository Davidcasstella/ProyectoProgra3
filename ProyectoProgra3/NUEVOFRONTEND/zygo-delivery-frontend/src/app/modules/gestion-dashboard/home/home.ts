// src/app/modules/gestion-dashboard/home/home.ts

import { Component, OnInit, signal, inject, computed, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NavbarComponent } from '../../../components/navbar/navbar';
import { PedidoService } from '../../../services/pedido.service';
import { RutaService } from '../../../services/ruta.service';
import { UsuarioService } from '../../../services/usuario.service';
import { AuthService } from '../../../services/auth.service';
import { Pedido } from '../../../models/pedido.model';
import { Usuario, UsuarioDTO } from '../../../models/usuario.model';
import { HttpClient } from '@angular/common/http';


// Importar Leaflet
import * as L from 'leaflet';
import { environment } from '../../../../environments/environment';

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
    RouterModule,
    NavbarComponent
  ],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class HomeComponent implements OnInit, AfterViewInit {
  private pedidoService = inject(PedidoService);
  private rutaService = inject(RutaService);
  private usuarioService = inject(UsuarioService);
  private authService = inject(AuthService);
  private http = inject(HttpClient);

  usuarioActual = this.authService.currentUserSignal;

  pedidos = signal<Pedido[]>([]);
  usuarios = signal<UsuarioDTO[]>([]);
  loading = signal(true);
  
  // ✨ NUEVAS VARIABLES PARA CONTROL DE RUTAS
  rutaCargando = signal(false);
  errorRuta = signal('');
  
  // VARIABLES EXISTENTES PARA EL MAPA Y HISTORIAL
  map: L.Map | null = null;
  currentPolyline: L.Polyline | null = null;
  currentMarkers: L.Marker[] = [];
  pedidoSeleccionado = signal<Pedido | null>(null);
  historialVisible = signal(true);

  stats = computed<StatCard[]>(() => {
    const user = this.usuarioActual();
    const totalPedidos = this.pedidos().length;
    const pedidosPendientes = this.pedidos().filter(p => p.estado === 'PENDIENTE').length;
    const pedidosEntregados = this.pedidos().filter(p => p.estado === 'ENTREGADO').length;
    const totalUsuarios = this.usuarios().length;

    // 🎯 ESTADÍSTICAS SEGÚN ROL
    if (user?.tipoUsuario === 'ADMIN') {
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
    }

    if (user?.tipoUsuario === 'CLIENTE') {
      const enCamino = this.pedidos().filter(p => p.estado === 'EN_CAMINO').length;
      return [
        {
          title: 'Mis Pedidos',
          label: 'Mis Pedidos',
          value: totalPedidos.toString(),
          change: 'Total de pedidos',
          icon: 'M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z',
          svg: '<path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/>',
          color: '#8b5cf6'
        },
        {
          title: 'Pendientes',
          label: 'Pendientes',
          value: pedidosPendientes.toString(),
          change: 'Esperando asignación',
          icon: 'M12 8v4l3 3m6-3a9 9 0 1 1-18 0 9 9 0 0 1 18 0z',
          svg: '<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>',
          color: '#f59e0b'
        },
        {
          title: 'En Camino',
          label: 'En Camino',
          value: enCamino.toString(),
          change: 'Siendo entregados',
          icon: 'M16 8a6 6 0 0 1 6 6v7h-4v-7a2 2 0 0 0-2-2',
          svg: '<path d="M16 8a6 6 0 0 1 6 6v7h-4v-7a2 2 0 0 0-2-2"/>',
          color: '#3b82f6'
        },
        {
          title: 'Entregados',
          label: 'Entregados',
          value: pedidosEntregados.toString(),
          change: 'Completados',
          icon: 'M9 12l2 2 4-4m6 2a9 9 0 1 1-18 0',
          svg: '<path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>',
          color: '#10b981'
        }
      ];
    }

    if (user?.tipoUsuario === 'REPARTIDOR') {
      const enCamino = this.pedidos().filter(p => p.estado === 'EN_CAMINO').length;
      return [
        {
          title: 'Mis Entregas',
          label: 'Mis Entregas',
          value: totalPedidos.toString(),
          change: 'Pedidos asignados',
          icon: 'M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z',
          svg: '<path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/>',
          color: '#8b5cf6'
        },
        {
          title: 'Por Recoger',
          label: 'Por Recoger',
          value: pedidosPendientes.toString(),
          change: 'Pendientes',
          icon: 'M12 8v4l3 3',
          svg: '<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>',
          color: '#f59e0b'
        },
        {
          title: 'En Ruta',
          label: 'En Ruta',
          value: enCamino.toString(),
          change: 'En camino',
          icon: 'M16 8a6 6 0 0 1 6 6',
          svg: '<path d="M16 8a6 6 0 0 1 6 6v7h-4v-7a2 2 0 0 0-2-2"/>',
          color: '#3b82f6'
        },
        {
          title: 'Completados',
          label: 'Completados',
          value: pedidosEntregados.toString(),
          change: 'Entregados',
          icon: 'M9 12l2 2 4-4',
          svg: '<polyline points="22 4 12 14.01 9 11.01"/>',
          color: '#10b981'
        }
      ];
    }

    return [];
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

  pedidosConCoordenadas = computed(() => {
    return this.pedidos()
      .filter(p => p.latOrigen != null && p.lonOrigen != null)
      .sort((a, b) => {
        const dateA = a.fechaCreacion ? new Date(a.fechaCreacion).getTime() : 0;
        const dateB = b.fechaCreacion ? new Date(b.fechaCreacion).getTime() : 0;
        return dateB - dateA;
      });
  });

  estadisticas = this.stats;
  pedidosRecientes = this.recentPedidos;

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
    
    this.rutaService.healthCheck().subscribe({
      next: (health) => console.log('✅ Servicio de rutas disponible:', health),
      error: (e) => console.warn('⚠️ Servicio de rutas no disponible:', e)
    });
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.inicializarMapa();
    }, 500);
  }

  // ✨ NUEVO - Cargar datos según el ROL del usuario
  cargarDatos(): void {
    this.loading.set(true);
    const user = this.usuarioActual();

    console.log('🔍 Cargando datos para rol:', user?.tipoUsuario);

    // 🎯 CARGAR PEDIDOS SEGÚN ROL
    if (user?.tipoUsuario === 'ADMIN') {
      // ADMIN: Ver todos los pedidos
      this.pedidoService.obtenerTodos().subscribe({
        next: (data: Pedido[]) => {
          console.log('📦 [ADMIN] Pedidos cargados:', data.length);
          this.pedidos.set(data);
        },
        error: (error: any) => {
          console.error('❌ Error al cargar pedidos:', error);
        }
      });

      // ADMIN: Ver todos los usuarios
      this.usuarioService.obtenerTodos().subscribe({
        next: (data: UsuarioDTO[]) => {
          console.log('👥 [ADMIN] Usuarios cargados:', data.length);
          this.usuarios.set(data);
          this.loading.set(false);
        },
        error: (error: any) => {
          console.error('❌ Error al cargar usuarios:', error);
          this.loading.set(false);
        }
      });

    } else if (user?.tipoUsuario === 'CLIENTE') {
      // CLIENTE: Solo sus propios pedidos
      this.http.get<Pedido[]>(`${environment.apiUrl}/cliente/mis-pedidos`).subscribe({
        next: (data: Pedido[]) => {
          console.log('📦 [CLIENTE] Mis pedidos cargados:', data.length);
          this.pedidos.set(data);
          this.loading.set(false);
        },
        error: (error: any) => {
          console.error('❌ Error al cargar mis pedidos:', error);
          this.loading.set(false);
        }
      });

    } else if (user?.tipoUsuario === 'REPARTIDOR') {
      // REPARTIDOR: Solo pedidos asignados a él
      this.http.get<Pedido[]>(`${environment.apiUrl}/repartidor/mis-pedidos`).subscribe({
        next: (data: Pedido[]) => {
          console.log('📦 [REPARTIDOR] Mis pedidos asignados:', data.length);
          this.pedidos.set(data);
          this.loading.set(false);
        },
        error: (error: any) => {
          console.error('❌ Error al cargar pedidos asignados:', error);
          this.loading.set(false);
        }
      });
    }
  }

  inicializarMapa(): void {
    const mapElement = document.getElementById('historialMap');
    if (!mapElement) {
      console.warn('⚠️ Elemento del mapa no encontrado');
      return;
    }

    try {
      this.map = L.map('historialMap').setView([5.7167, -72.9347], 13);

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors',
        maxZoom: 19
      }).addTo(this.map);

      console.log('✅ Mapa inicializado correctamente');
    } catch (error) {
      console.error('❌ Error al inicializar mapa:', error);
    }
  }

  dibujarRutaPedido(pedido: Pedido): void {
    if (!this.map) {
      console.warn('⚠️ Mapa no inicializado');
      return;
    }

    if (!pedido.latOrigen || !pedido.lonOrigen || !pedido.latDestino || !pedido.lonDestino) {
      console.warn('⚠️ Pedido sin coordenadas completas:', pedido.id);
      return;
    }

    const latOrigen = pedido.latOrigen as number;
    const lonOrigen = pedido.lonOrigen as number;
    const latDestino = pedido.latDestino as number;
    const lonDestino = pedido.lonDestino as number;

    this.limpiarRuta();

    this.rutaCargando.set(true);
    this.errorRuta.set('');

    console.log('🚀 Calculando ruta óptima con Dijkstra...');

    this.rutaService.calcularRutaOptima(
      latOrigen,
      lonOrigen,
      latDestino,
      lonDestino
    ).subscribe({
      next: (respuesta) => {
        this.rutaCargando.set(false);
        console.log('✅ Ruta calculada en', respuesta.tiempoCalculoMs, 'ms');
        
        this.dibujarPolylineDesdeRuta(respuesta.ruta, pedido);
        this.dibujarMarcadores(pedido);
        this.centrarMapaEnRuta(respuesta.ruta);
        this.pedidoSeleccionado.set(pedido);
      },
      error: (error) => {
        this.rutaCargando.set(false);
        const mensajeError = error?.error?.error || error?.message || 'Error desconocido';
        this.errorRuta.set(mensajeError);
        
        console.error('❌ Error al calcular ruta:', error);
        console.warn('⚠️ Dibujando línea recta como fallback...');
        
        this.dibujarLineaRecta(pedido);
      }
    });
  }

  private dibujarPolylineDesdeRuta(ruta: any, pedido: Pedido): void {
  if (!this.map) return;

  const coordenadas: L.LatLngExpression[] = ruta.nodos.map((nodo: any) => 
    [nodo.latitud, nodo.longitud] as L.LatLngExpression
  );

  const colorRuta = this.getColorForEstado(pedido.estado || 'PENDIENTE');

  this.currentPolyline = L.polyline(coordenadas, {
    color: colorRuta,
    weight: 4,
    opacity: 0.8,
    dashArray: '5, 10',
    lineJoin: 'round',
    lineCap: 'round',
    interactive: false // ✅ Evita que capture eventos
  }).addTo(this.map);

  // ✅ Enviar polyline al fondo
  if (this.currentPolyline) {
    this.currentPolyline.bringToBack();
  }
}

 private dibujarLineaRecta(pedido: Pedido): void {
  if (!this.map) return;

  const latOrigen = pedido.latOrigen as number;
  const lonOrigen = pedido.lonOrigen as number;
  const latDestino = pedido.latDestino as number;
  const lonDestino = pedido.lonDestino as number;

  const origen: L.LatLngExpression = [latOrigen, lonOrigen];
  const destino: L.LatLngExpression = [latDestino, lonDestino];
  
  const colorRuta = this.getColorForEstado(pedido.estado || 'PENDIENTE');

  this.currentPolyline = L.polyline([origen, destino], {
    color: colorRuta,
    weight: 2,
    opacity: 0.5,
    dashArray: '2, 4',
    interactive: false // ✅ Evita que capture eventos
  }).addTo(this.map);

  // ✅ Enviar polyline al fondo
  if (this.currentPolyline) {
    this.currentPolyline.bringToBack();
  }

  this.dibujarMarcadores(pedido);
  
  const bounds = L.latLngBounds([origen, destino]);
  this.map.fitBounds(bounds, { padding: [50, 50] });
  
  this.pedidoSeleccionado.set(pedido);
}

 private dibujarMarcadores(pedido: Pedido): void {
  if (!this.map) return;

  const latOrigen = pedido.latOrigen as number;
  const lonOrigen = pedido.lonOrigen as number;
  const latDestino = pedido.latDestino as number;
  const lonDestino = pedido.lonDestino as number;

  // 🔵 Icono del Repartidor (Origen - Azul)
  const iconoRepartidor = L.divIcon({
    className: 'custom-marker',
    html: `
      <div style="
        background: linear-gradient(135deg, #3b82f6, #2563eb);
        width: 32px;
        height: 32px;
        border-radius: 50% 50% 50% 0;
        transform: rotate(-45deg);
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 4px 12px rgba(59, 130, 246, 0.5);
        border: 2px solid white;
      ">
        <svg style="transform: rotate(45deg); width: 16px; height: 16px; color: white;" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="5.5" cy="17.5" r="3.5"/>
          <circle cx="18.5" cy="17.5" r="3.5"/>
          <path d="M12 17.5V14l-3-3 4-3 2 3h2"/>
        </svg>
      </div>
    `,
    iconSize: [32, 32],
    iconAnchor: [16, 32]
  });

  // 🟠 Icono del Restaurante (Punto medio - Naranja)
  const iconoRestaurante = L.divIcon({
    className: 'custom-marker',
    html: `
      <div style="
        background: linear-gradient(135deg, #f59e0b, #fbbf24);
        width: 32px;
        height: 32px;
        border-radius: 50% 50% 50% 0;
        transform: rotate(-45deg);
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 4px 12px rgba(245, 158, 11, 0.5);
        border: 2px solid white;
      ">
        <svg style="transform: rotate(45deg); width: 16px; height: 16px; color: white;" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M3 2v7c0 1.1.9 2 2 2h4a2 2 0 002-2V2M7 2v20M21 15V2v0a5 5 0 00-5 5v6c0 1.1.9 2 2 2h3z"/>
        </svg>
      </div>
    `,
    iconSize: [32, 32],
    iconAnchor: [16, 32]
  });

  // 🟢 Icono del Cliente (Destino - Verde)
  const iconoCliente = L.divIcon({
    className: 'custom-marker',
    html: `
      <div style="
        background: linear-gradient(135deg, #10b981, #34d399);
        width: 32px;
        height: 32px;
        border-radius: 50% 50% 50% 0;
        transform: rotate(-45deg);
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 4px 12px rgba(16, 185, 129, 0.5);
        border: 2px solid white;
      ">
        <svg style="transform: rotate(45deg); width: 16px; height: 16px; color: white;" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
        </svg>
      </div>
    `,
    iconSize: [32, 32],
    iconAnchor: [16, 32]
  });

  // 🔵 Marcador del Repartidor (Origen)
  const markerRepartidor = L.marker([latOrigen, lonOrigen], { 
    icon: iconoRepartidor,
    zIndexOffset: 1000
  })
    .bindPopup(`
      <div style="text-align: center;">
        <strong>🚴 Repartidor</strong><br>
        <small>${pedido.nombreRepartidor || 'Repartidor asignado'}</small><br>
        <small style="color: #3b82f6;">📍 Punto de inicio</small>
      </div>
    `)
    .addTo(this.map);

  this.currentMarkers = [markerRepartidor];

  // 🟠 Marcador del Restaurante (si existe)
  if (pedido.latRestaurante != null && pedido.lonRestaurante != null) {
    const markerRestaurante = L.marker(
      [pedido.latRestaurante, pedido.lonRestaurante], 
      { 
        icon: iconoRestaurante,
        zIndexOffset: 1000
      }
    )
      .bindPopup(`
        <div style="text-align: center;">
          <strong>🍽️ Restaurante</strong><br>
          <small>${pedido.nombreRestaurante || 'Restaurante'}</small><br>
          <small style="color: #f59e0b;">📦 Recoger pedido aquí</small>
        </div>
      `)
      .addTo(this.map);
    
    this.currentMarkers.push(markerRestaurante);
  }

  // 🟢 Marcador del Cliente (Destino)
  const markerCliente = L.marker([latDestino, lonDestino], { 
    icon: iconoCliente,
    zIndexOffset: 1000
  })
    .bindPopup(`
      <div style="text-align: center;">
        <strong>🏠 Cliente</strong><br>
        <small>${pedido.nombreCliente || 'Cliente'}</small><br>
        <small style="color: #10b981;">🎯 Destino de entrega</small>
      </div>
    `)
    .addTo(this.map);

  this.currentMarkers.push(markerCliente);
}
  private centrarMapaEnRuta(ruta: any): void {
    if (!this.map || !ruta.nodos || ruta.nodos.length === 0) return;

    const coordenadas = ruta.nodos.map((nodo: any) => 
      [nodo.latitud, nodo.longitud] as L.LatLngExpression
    );

    const bounds = L.latLngBounds(coordenadas);
    this.map.fitBounds(bounds, { padding: [50, 50] });
  }

  limpiarRuta(): void {
    if (!this.map) return;

    if (this.currentPolyline) {
      this.map.removeLayer(this.currentPolyline);
      this.currentPolyline = null;
    }

    this.currentMarkers.forEach(marker => {
      this.map?.removeLayer(marker);
    });
    this.currentMarkers = [];

    this.pedidoSeleccionado.set(null);
    this.errorRuta.set('');
  }

  toggleHistorial(): void {
    this.historialVisible.update(v => !v);
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