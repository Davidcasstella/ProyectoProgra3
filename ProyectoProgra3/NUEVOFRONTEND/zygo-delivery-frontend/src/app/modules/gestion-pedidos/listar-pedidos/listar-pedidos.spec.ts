// src/app/modules/gestion-pedidos/listar-pedidos/listar-pedidos.ts

import { Component, OnInit, ViewChild, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { PedidoService } from '../../../services/pedido.service';
import { LugarService } from '../../../services/lugar.service';
import { AuthService } from '../../../services/auth.service';
import { Pedido, EstadoPedido } from '../../../models/pedido.model';
import { SelectorUbicacionComponent } from '../../gestion-mapas/selector-ubicacion/selector-ubicacion';
import { interval, Subscription } from 'rxjs';

@Component({
  selector: 'app-listar-pedidos',
  standalone: true,
  imports: [CommonModule, SelectorUbicacionComponent],
  templateUrl: './listar-pedidos.html',
  styleUrls: ['./listar-pedidos.css']
})
export class ListarPedidos implements OnInit, OnDestroy {
  @ViewChild(SelectorUbicacionComponent) mapaComponent!: SelectorUbicacionComponent;

  private pedidoService = inject(PedidoService);
  private lugarService = inject(LugarService);
  private authService = inject(AuthService);
  private router = inject(Router);

  pedidos: Pedido[] = [];
  pedidosFiltrados: Pedido[] = [];
  cargando = true;
  error: string | null = null;

  // Modal de ruta
  mostrarModalRuta = false;
  pedidoSeleccionado: Pedido | null = null;
  rutaCalculada: any = null;
  calculandoRuta = false;

  // Filtros
  filtroEstado: string = 'TODOS';
  estadosDisponibles = ['TODOS', 'PENDIENTE', 'ASIGNADO', 'EN_CAMINO', 'ENTREGADO'];

  // Auto-actualización
  private autoRefreshSubscription?: Subscription;
  private readonly REFRESH_INTERVAL = 30000; // 30 segundos

  // Usuario actual
  get usuarioActual() {
    return this.authService.currentUserSignal();
  }

  ngOnInit(): void {
    this.cargarPedidos();
    this.iniciarAutoActualizacion();
  }

  ngOnDestroy(): void {
    // Limpiar suscripción al destruir componente
    if (this.autoRefreshSubscription) {
      this.autoRefreshSubscription.unsubscribe();
    }
  }

  /**
   * Iniciar actualización automática cada 30 segundos
   */
  iniciarAutoActualizacion(): void {
    this.autoRefreshSubscription = interval(this.REFRESH_INTERVAL)
      .subscribe(() => {
        console.log('🔄 Auto-actualizando lista de pedidos...');
        this.cargarPedidos(true); // true = silencioso (sin mostrar loading)
      });
  }

  /**
   * Carga los pedidos del repartidor actual
   */
  cargarPedidos(silencioso: boolean = false): void {
    if (!silencioso) {
      this.cargando = true;
    }
    this.error = null;

    const usuario = this.usuarioActual;
    if (!usuario) {
      this.error = 'No hay usuario autenticado';
      this.cargando = false;
      return;
    }

    // Si es REPARTIDOR, mostrar solo sus pedidos
    // Si es ADMIN/CLIENTE, mostrar todos los pedidos
    if (usuario.tipo === 'REPARTIDOR' && usuario.id) {
      this.pedidoService.obtenerPorRepartidor(usuario.id).subscribe({
        next: (pedidos) => {
          this.pedidos = pedidos;
          this.aplicarFiltros();
          this.cargando = false;
          if (!silencioso) {
            console.log('✅ Pedidos del repartidor cargados:', pedidos.length);
          }
        },
        error: (err) => {
          this.error = 'Error al cargar pedidos';
          this.cargando = false;
          console.error('❌ Error al cargar pedidos:', err);
        }
      });
    } else {
      // Si es ADMIN o CLIENTE, mostrar todos
      this.pedidoService.obtenerTodos().subscribe({
        next: (pedidos) => {
          this.pedidos = pedidos;
          this.aplicarFiltros();
          this.cargando = false;
          if (!silencioso) {
            console.log('✅ Todos los pedidos cargados:', pedidos.length);
          }
        },
        error: (err) => {
          this.error = 'Error al cargar pedidos';
          this.cargando = false;
          console.error('❌ Error al cargar pedidos:', err);
        }
      });
    }
  }

  /**
   * Aplica filtros según el estado seleccionado
   */
  aplicarFiltros(): void {
    if (this.filtroEstado === 'TODOS') {
      this.pedidosFiltrados = [...this.pedidos];
    } else {
      this.pedidosFiltrados = this.pedidos.filter(
        p => p.estado === this.filtroEstado
      );
    }
  }

  /**
   * Cambia el filtro de estado
   */
  cambiarFiltro(estado: string): void {
    this.filtroEstado = estado;
    this.aplicarFiltros();
  }

  /**
   * Abre el modal con la ruta del pedido
   */
  verRuta(pedido: Pedido): void {
    console.log('🗺️ Abriendo ruta para pedido:', pedido);
    
    this.pedidoSeleccionado = pedido;
    this.mostrarModalRuta = true;
    this.rutaCalculada = null;

    // Si el pedido tiene coordenadas, calcular ruta
    if (pedido.latOrigen && pedido.lonOrigen && pedido.latDestino && pedido.lonDestino) {
      console.log('✅ Pedido tiene coordenadas, calculando ruta...');
      this.calcularRutaPedido(pedido);
    } else {
      console.warn('⚠️ El pedido no tiene coordenadas guardadas');
    }
  }

  /**
   * Calcula la ruta del pedido usando las coordenadas
   */
  calcularRutaPedido(pedido: Pedido): void {
    if (!pedido.latOrigen || !pedido.lonOrigen || !pedido.latDestino || !pedido.lonDestino) {
      return;
    }

    this.calculandoRuta = true;
    console.log('⏳ Calculando ruta para pedido:', pedido.id);

    const request = {
      latOrigen: pedido.latOrigen,
      lonOrigen: pedido.lonOrigen,
      latDestino: pedido.latDestino,
      lonDestino: pedido.lonDestino
    };

    this.lugarService.calcularRutaPorCoordenadas(request).subscribe({
      next: (ruta: any) => {
        this.rutaCalculada = ruta;
        this.calculandoRuta = false;
        console.log('✅ Ruta calculada:', ruta);

        // Dibujar la ruta en el mapa si está disponible
        setTimeout(() => {
          if (this.mapaComponent && ruta.nodos) {
            const coordenadas = ruta.nodos.map((nodo: any) => ({
              lat: nodo.latitud,
              lng: nodo.longitud
            }));
            
            if (typeof this.mapaComponent.dibujarRuta === 'function') {
              this.mapaComponent.dibujarRuta({ nodos: ruta.nodos } as any);
            }
          }
        }, 500);
      },
      error: (err) => {
        this.calculandoRuta = false;
        console.error('❌ Error al calcular ruta:', err);
      }
    });
  }

  /**
   * Cierra el modal de ruta
   */
  cerrarModalRuta(): void {
    this.mostrarModalRuta = false;
    this.pedidoSeleccionado = null;
    this.rutaCalculada = null;
  }

  /**
   * Cambia el estado del pedido
   */
  cambiarEstado(pedido: Pedido, nuevoEstado: string): void {
    if (!pedido.id) return;

    console.log(`🔄 Cambiando estado de pedido ${pedido.id} a ${nuevoEstado}`);

    this.pedidoService.actualizarEstado(pedido.id, nuevoEstado).subscribe({
      next: (pedidoActualizado) => {
        console.log('✅ Estado actualizado:', pedidoActualizado);
        
        // Actualizar en la lista
        const index = this.pedidos.findIndex(p => p.id === pedido.id);
        if (index !== -1) {
          this.pedidos[index] = pedidoActualizado;
          this.aplicarFiltros();
        }

        // Si estamos en el modal, actualizar el pedido seleccionado
        if (this.pedidoSeleccionado?.id === pedido.id) {
          this.pedidoSeleccionado = pedidoActualizado;
        }

        // Cerrar modal si el pedido fue entregado
        if (nuevoEstado === 'ENTREGADO') {
          this.cerrarModalRuta();
        }
      },
      error: (err) => {
        console.error('❌ Error al actualizar estado:', err);
        alert('Error al actualizar el estado del pedido');
      }
    });
  }

  /**
   * Obtiene el color del badge según el estado
   */
  getEstadoColor(estado?: EstadoPedido): string {
    switch (estado) {
      case EstadoPedido.PENDIENTE:
        return 'badge-warning';
      case EstadoPedido.ASIGNADO:
        return 'badge-info';
      case EstadoPedido.EN_CAMINO:
        return 'badge-primary';
      case EstadoPedido.ENTREGADO:
        return 'badge-success';
      case EstadoPedido.CANCELADO:
        return 'badge-danger';
      default:
        return 'badge-secondary';
    }
  }

  /**
   * Obtiene el ícono según el estado
   */
  getEstadoIcono(estado?: EstadoPedido): string {
    switch (estado) {
      case EstadoPedido.PENDIENTE:
        return '⏳';
      case EstadoPedido.ASIGNADO:
        return '📋';
      case EstadoPedido.EN_CAMINO:
        return '🚴';
      case EstadoPedido.ENTREGADO:
        return '✅';
      case EstadoPedido.CANCELADO:
        return '❌';
      default:
        return '📦';
    }
  }

  /**
   * Verifica si se puede cambiar a EN_CAMINO
   */
  puedeMarcarEnCamino(pedido: Pedido): boolean {
    return pedido.estado === EstadoPedido.ASIGNADO;
  }

  /**
   * Verifica si se puede cambiar a ENTREGADO
   */
  puedeMarcarEntregado(pedido: Pedido): boolean {
    return pedido.estado === EstadoPedido.EN_CAMINO;
  }
}