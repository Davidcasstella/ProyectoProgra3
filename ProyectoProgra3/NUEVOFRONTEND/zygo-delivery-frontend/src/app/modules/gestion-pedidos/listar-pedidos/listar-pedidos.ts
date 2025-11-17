// src/app/modules/gestion-pedidos/listar-pedidos/listar-pedidos.ts

import { Component, OnInit, OnDestroy, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { interval, Subscription } from 'rxjs';
import { PedidoService } from '../../../services/pedido.service';
import { LugarService } from '../../../services/lugar.service';
import { AuthService } from '../../../services/auth.service';
import { Pedido, EstadoPedido } from '../../../models/pedido.model';
import { SelectorUbicacionComponent } from '../../gestion-mapas/selector-ubicacion/selector-ubicacion';

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

  // ✅ Auto-actualización
  private refreshSubscription?: Subscription;
  private readonly REFRESH_INTERVAL = 10000; // 10 segundos

  get usuarioActual() {
    return this.authService.currentUserSignal();
  }

  ngOnInit(): void {
    console.log('🚀 Iniciando ListarPedidos...');
    this.cargarPedidos();
    this.iniciarAutoActualizacion();
  }

  ngOnDestroy(): void {
    console.log('🛑 Destruyendo ListarPedidos...');
    this.detenerAutoActualizacion();
  }

  /**
   * ✅ Inicia la actualización automática cada 10 segundos
   */
  iniciarAutoActualizacion(): void {
    this.refreshSubscription = interval(this.REFRESH_INTERVAL).subscribe(() => {
      console.log('🔄 Auto-actualizando lista de pedidos...');
      this.cargarPedidos(true); // true = actualización silenciosa
    });
  }

  /**
   * ✅ Detiene la actualización automática
   */
  detenerAutoActualizacion(): void {
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }
  }

  /**
   * ✅ Carga los pedidos (con opción de actualización silenciosa)
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

  console.log('📡 Solicitando pedidos al backend...');
  console.log('👤 Usuario:', usuario.tipo, usuario.id);

  // ✅ SOLUCIÓN SIMPLE: Siempre cargar TODOS los pedidos
  const observable = this.pedidoService.obtenerTodos();

  observable.subscribe({
    next: (pedidos) => {
      console.log('📦 Respuesta del backend:', pedidos);
      console.log('📊 Total de pedidos:', pedidos.length);
      
      const ids = pedidos.map(p => p.id);
      console.log('🔢 IDs de pedidos:', ids);
      
      const pedidosAnteriores = this.pedidos.length;
      this.pedidos = pedidos;
      this.aplicarFiltros();
      this.cargando = false;

      if (!silencioso) {
        console.log(`✅ ${pedidos.length} pedidos cargados`);
      } else if (pedidos.length !== pedidosAnteriores) {
        console.log(`🔄 Lista actualizada: ${pedidosAnteriores} → ${pedidos.length} pedidos`);
      }
    },
    error: (err) => {
      this.error = 'Error al cargar pedidos';
      this.cargando = false;
      console.error('❌ Error al cargar pedidos:', err);
    }
  });
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

    if (pedido.latOrigen && pedido.lonOrigen && pedido.latDestino && pedido.lonDestino) {
      this.calcularRutaPedido(pedido);
    } else {
      console.warn('⚠️ El pedido no tiene coordenadas guardadas');
      alert('Este pedido no tiene coordenadas de mapa. Fue creado antes de implementar esta funcionalidad.');
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

        setTimeout(() => {
          if (this.mapaComponent && ruta.camino) {
            const coordenadas = ruta.camino.map((nodo: any) => ({
              lat: nodo.latitud,
              lng: nodo.longitud
            }));
            
            if (typeof this.mapaComponent.dibujarRuta === 'function') {
              this.mapaComponent.dibujarRuta(coordenadas);
            } else if (typeof (this.mapaComponent as any).mostrarRuta === 'function') {
              (this.mapaComponent as any).mostrarRuta(coordenadas);
            }
          }
        }, 500);
      },
      error: (err) => {
        this.calculandoRuta = false;
        console.error('❌ Error al calcular ruta:', err);
        alert('Error al calcular la ruta');
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
        
        const index = this.pedidos.findIndex(p => p.id === pedido.id);
        if (index !== -1) {
          this.pedidos[index] = pedidoActualizado;
          this.aplicarFiltros();
        }

        if (this.pedidoSeleccionado?.id === pedido.id) {
          this.pedidoSeleccionado = pedidoActualizado;
          
          // ✅ Cerrar modal automáticamente si se marca como ENTREGADO
          if (nuevoEstado === 'ENTREGADO') {
            setTimeout(() => {
              this.cerrarModalRuta();
            }, 1500);
          }
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

  /**
   * ✅ Refrescar manualmente
   */
  refrescarLista(): void {
    console.log('🔄 Refrescando lista manualmente...');
    this.cargarPedidos();
  }
}