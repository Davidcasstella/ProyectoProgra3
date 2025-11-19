// src/app/modules/gestion-dashboard/home/home.ts

import { Component, OnInit, signal, inject, computed, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NavbarComponent } from '../../../components/navbar/navbar';
import { PedidoService } from '../../../services/pedido.service';
import { RutaService } from '../../../services/ruta.service'; // ✨ NUEVO
import { UsuarioService } from '../../../services/usuario.service';
import { AuthService } from '../../../services/auth.service';
import { Pedido } from '../../../models/pedido.model';
import { Usuario, UsuarioDTO } from '../../../models/usuario.model';

// Importar Leaflet
import * as L from 'leaflet';

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
  private rutaService = inject(RutaService); // ✨ NUEVO - Inyectar servicio de rutas
  private usuarioService = inject(UsuarioService);
  private authService = inject(AuthService);

  usuarioActual = this.authService.currentUserSignal;

  pedidos = signal<Pedido[]>([]);
  usuarios = signal<UsuarioDTO[]>([]);
  loading = signal(true);
  
  // ✨ NUEVAS VARIABLES PARA CONTROL DE RUTAS
  rutaCargando = signal(false); // Spinner de carga
  errorRuta = signal(''); // Mensaje de error
  
  // VARIABLES EXISTENTES PARA EL MAPA Y HISTORIAL
  map: L.Map | null = null;
  currentPolyline: L.Polyline | null = null;
  currentMarkers: L.Marker[] = [];
  pedidoSeleccionado = signal<Pedido | null>(null);
  historialVisible = signal(true);

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

  pedidosConCoordenadas = computed(() => {
    return this.pedidos()
      .filter(p => p.latOrigen != null && p.lonOrigen != null)
      .sort((a, b) => {
        const dateA = a.fechaCreacion ? new Date(a.fechaCreacion).getTime() : 0;
        const dateB = b.fechaCreacion ? new Date(b.fechaCreacion).getTime() : 0;
        return dateB - dateA;
      });
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
    
    // ✨ NUEVO - Health check del servicio de rutas
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

  cargarDatos(): void {
    this.loading.set(true);

    this.pedidoService.obtenerTodos().subscribe({
      next: (data: Pedido[]) => {
        console.log('📦 Pedidos cargados:', data.length);
        this.pedidos.set(data);
      },
      error: (error: any) => {
        console.error('❌ Error al cargar pedidos:', error);
      }
    });

    this.usuarioService.obtenerTodos().subscribe({
      next: (data: UsuarioDTO[]) => {
        this.usuarios.set(data);
        this.loading.set(false);
      },
      error: (error: any) => {
        console.error('❌ Error al cargar usuarios:', error);
        this.loading.set(false);
      }
    });
  }

  inicializarMapa(): void {
    const mapElement = document.getElementById('historialMap');
    if (!mapElement) {
      console.warn('⚠️ Elemento del mapa no encontrado');
      return;
    }

    try {
      // Crear mapa centrado en Sogamoso, Colombia
      this.map = L.map('historialMap').setView([5.7167, -72.9347], 13);

      // Capa de OpenStreetMap
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors',
        maxZoom: 19
      }).addTo(this.map);

      console.log('✅ Mapa inicializado correctamente');
    } catch (error) {
      console.error('❌ Error al inicializar mapa:', error);
    }
  }

  /**
   * ✨ NUEVO - Dibuja ruta real usando Dijkstra del backend
   * Reemplaza el antiguo método dibujarRutaPedido()
   */
  dibujarRutaPedido(pedido: Pedido): void {
    if (!this.map) {
      console.warn('⚠️ Mapa no inicializado');
      return;
    }

    // ✅ Validar que todas las coordenadas existan
    if (!pedido.latOrigen || !pedido.lonOrigen || !pedido.latDestino || !pedido.lonDestino) {
      console.warn('⚠️ Pedido sin coordenadas completas:', pedido.id);
      return;
    }

    // ✅ Cast seguro de coordenadas
    const latOrigen = pedido.latOrigen as number;
    const lonOrigen = pedido.lonOrigen as number;
    const latDestino = pedido.latDestino as number;
    const lonDestino = pedido.lonDestino as number;

    // Limpiar ruta anterior
    this.limpiarRuta();

    this.rutaCargando.set(true);
    this.errorRuta.set('');

    console.log('🚀 Calculando ruta óptima con Dijkstra...');
    console.log('📍 Origen:', [latOrigen, lonOrigen]);
    console.log('📍 Destino:', [latDestino, lonDestino]);

    // ✨ NUEVO - Llamar servicio de rutas para obtener coordenadas reales
    this.rutaService.calcularRutaOptima(
      latOrigen,
      lonOrigen,
      latDestino,
      lonDestino
    ).subscribe({
      next: (respuesta) => {
        this.rutaCargando.set(false);
        console.log('✅ Ruta calculada en', respuesta.tiempoCalculoMs, 'ms');
        console.log('📊 Resumen:', respuesta.resumen);
        
        // Dibujar ruta con todas las coordenadas
        this.dibujarPolylineDesdeRuta(respuesta.ruta, pedido);
        
        // Dibujar marcadores
        this.dibujarMarcadores(pedido);
        
        // Centrar mapa en la ruta
        this.centrarMapaEnRuta(respuesta.ruta);
        
        // Marcar como seleccionado
        this.pedidoSeleccionado.set(pedido);
      },
      error: (error) => {
        this.rutaCargando.set(false);
        const mensajeError = error?.error?.error || error?.message || 'Error desconocido';
        this.errorRuta.set(mensajeError);
        
        console.error('❌ Error al calcular ruta:', error);
        console.warn('⚠️ Dibujando línea recta como fallback...');
        
        // FALLBACK: Dibujar línea recta si hay error
        this.dibujarLineaRecta(pedido);
      }
    });
  }

  /**
   * ✨ NUEVO - Dibuja polyline usando las coordenadas de la ruta calculada
   */
  private dibujarPolylineDesdeRuta(ruta: any, pedido: Pedido): void {
    if (!this.map) return;

    // Extraer coordenadas de los nodos
    const coordenadas: L.LatLngExpression[] = ruta.nodos.map((nodo: any) => 
      [nodo.latitud, nodo.longitud] as L.LatLngExpression
    );

    console.log('📍 Coordenadas de la ruta:', coordenadas.length, 'nodos');

    const colorRuta = this.getColorForEstado(pedido.estado || 'PENDIENTE');

    // Dibujar polyline con todas las coordenadas (sigue las calles)
    this.currentPolyline = L.polyline(coordenadas, {
      color: colorRuta,
      weight: 4,
      opacity: 0.8,
      dashArray: '5, 10',
      lineJoin: 'round',
      lineCap: 'round'
    }).addTo(this.map);

    console.log('✅ Ruta dibujada:', ruta.distanciaTotalKm.toFixed(2), 'km');
  }

  /**
   * ✨ NUEVO - FALLBACK: Dibuja línea recta si hay error
   */
  private dibujarLineaRecta(pedido: Pedido): void {
    if (!this.map) return;

    // ✅ Cast seguro de coordenadas
    const latOrigen = pedido.latOrigen as number;
    const lonOrigen = pedido.lonOrigen as number;
    const latDestino = pedido.latDestino as number;
    const lonDestino = pedido.lonDestino as number;

    const origen: L.LatLngExpression = [latOrigen, lonOrigen];
    const destino: L.LatLngExpression = [latDestino, lonDestino];
    
    const colorRuta = this.getColorForEstado(pedido.estado || 'PENDIENTE');

    // Dibujar línea recta simple
    this.currentPolyline = L.polyline([origen, destino], {
      color: colorRuta,
      weight: 2,
      opacity: 0.5,
      dashArray: '2, 4'
    }).addTo(this.map);

    this.dibujarMarcadores(pedido);
    
    const bounds = L.latLngBounds([origen, destino]);
    this.map.fitBounds(bounds, { padding: [50, 50] });
    
    this.pedidoSeleccionado.set(pedido);
  }

  /**
   * ✨ NUEVO - Dibuja marcadores de origen y destino
   */
  private dibujarMarcadores(pedido: Pedido): void {
    if (!this.map) return;

    // ✅ Cast seguro de coordenadas
    const latOrigen = pedido.latOrigen as number;
    const lonOrigen = pedido.lonOrigen as number;
    const latDestino = pedido.latDestino as number;
    const lonDestino = pedido.lonDestino as number;

    const iconoOrigen = L.divIcon({
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
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
          </svg>
        </div>
      `,
      iconSize: [32, 32],
      iconAnchor: [16, 32]
    });

    const iconoDestino = L.divIcon({
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

    const markerOrigen = L.marker([latOrigen, lonOrigen], { icon: iconoOrigen })
      .bindPopup(`<b>Origen</b><br>${pedido.direccionOrigen}`)
      .addTo(this.map);

    const markerDestino = L.marker([latDestino, lonDestino], { icon: iconoDestino })
      .bindPopup(`<b>Destino</b><br>${pedido.direccionDestino}`)
      .addTo(this.map);

    this.currentMarkers = [markerOrigen, markerDestino];

    console.log('✅ Marcadores dibujados');
  }

  /**
   * ✨ NUEVO - Centra el mapa en la ruta completa
   */
  private centrarMapaEnRuta(ruta: any): void {
    if (!this.map || !ruta.nodos || ruta.nodos.length === 0) return;

    const coordenadas = ruta.nodos.map((nodo: any) => 
      [nodo.latitud, nodo.longitud] as L.LatLngExpression
    );

    const bounds = L.latLngBounds(coordenadas);
    this.map.fitBounds(bounds, { padding: [50, 50] });

    console.log('🎥 Mapa centrado en la ruta');
  }

  limpiarRuta(): void {
    if (!this.map) return;

    // Eliminar polyline
    if (this.currentPolyline) {
      this.map.removeLayer(this.currentPolyline);
      this.currentPolyline = null;
    }

    // Eliminar marcadores
    this.currentMarkers.forEach(marker => {
      this.map?.removeLayer(marker);
    });
    this.currentMarkers = [];

    // Resetear selección
    this.pedidoSeleccionado.set(null);
    this.errorRuta.set('');

    console.log('🗑️ Ruta limpiada del mapa');
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