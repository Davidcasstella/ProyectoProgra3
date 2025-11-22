// src/app/modules/gestion-pedidos/detalle-ruta/detalle-ruta.ts
import { Component, OnInit, AfterViewInit, OnDestroy, ViewChild, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { PedidoService } from '../../../services/pedido.service';
import { LugarService } from '../../../services/lugar.service';
import { Pedido, EstadoPedido } from '../../../models/pedido.model';
import { SelectorUbicacionComponent } from '../../gestion-mapas/selector-ubicacion/selector-ubicacion';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-detalle-ruta',
  standalone: true,
  imports: [CommonModule, SelectorUbicacionComponent],
  templateUrl: './detalle-ruta.html',
  styleUrls: ['./detalle-ruta.css']
})
export class DetalleRuta implements OnInit {
  @ViewChild(SelectorUbicacionComponent) mapaComponent!: SelectorUbicacionComponent;

  private pedidoService = inject(PedidoService);
  private lugarService = inject(LugarService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);

  pedido: Pedido | null = null;
  cargando = true;
  error: string | null = null;

  // Rutas calculadas
  rutaPickup: any = null;  // Repartidor → Restaurante
  rutaDelivery: any = null; // Restaurante → Cliente
  calculandoRutas = false;

  // 🆕 Flag para controlar cuándo el mapa está listo
  private mapaListo = false;

  ngOnInit(): void {
  const pedidoId = this.route.snapshot.paramMap.get('id');
  if (pedidoId) {
    // Limpiar rutas anteriores
    this.rutaPickup = null;
    this.rutaDelivery = null;
    this.mapaListo = false;
    
    // ✅ Envolver en setTimeout para evitar NG0100
    setTimeout(() => {
      this.cargarPedido(Number(pedidoId));
    }, 0);
  }
}

  ngOnDestroy(): void {
    console.log('🛑 Destruyendo DetalleRuta...');
    // Limpiar polylines si existen
    if (this.mapaComponent) {
      this.limpiarRutasDelMapa();
    }
  }

  /**
   * 🆕 Limpiar rutas dibujadas en el mapa
   */
  private limpiarRutasDelMapa(): void {
    try {
      const map = (this.mapaComponent as any).map;
      if (!map) return;

      // Remover todas las polylines del mapa
      map.eachLayer((layer: any) => {
        if (layer instanceof (window as any).L.Polyline) {
          map.removeLayer(layer);
        }
      });

      console.log('🧹 Rutas limpiadas del mapa');
    } catch (error) {
      console.warn('⚠️ Error al limpiar rutas:', error);
    }
  }

  ngAfterViewInit(): void {
    // ✅ Esperar a que el mapa esté inicializado
    console.log('🗺️ AfterViewInit - Componente del mapa:', this.mapaComponent);
    
    // Dar tiempo al mapa para inicializarse completamente
    // Intentar múltiples veces si es necesario
    this.esperarMapaListo();
  }

  /**
   * 🆕 Esperar hasta que el mapa esté completamente inicializado
   */
  private esperarMapaListo(intentos: number = 0): void {
    const maxIntentos = 10;
    
    if (intentos >= maxIntentos) {
      console.error('❌ No se pudo inicializar el mapa después de', maxIntentos, 'intentos');
      return;
    }

    setTimeout(() => {
      console.log(`🔍 Intento ${intentos + 1}/${maxIntentos} - Verificando mapa...`);
      
      // Verificar que el componente existe
      if (!this.mapaComponent) {
        console.warn('⏳ Componente del mapa aún no existe, reintentando...');
        this.esperarMapaListo(intentos + 1);
        return;
      }

      // Verificar que el mapa interno de Leaflet existe
      const map = (this.mapaComponent as any).map;
      if (!map) {
        console.warn('⏳ Mapa interno de Leaflet aún no existe, reintentando...');
        this.esperarMapaListo(intentos + 1);
        return;
      }

      // ✅ Mapa está listo!
      this.mapaListo = true;
      console.log('✅ Mapa completamente inicializado y listo');
      
      // Si ya tenemos rutas calculadas, dibujarlas ahora
      if (this.rutaPickup || this.rutaDelivery) {
        console.log('🎨 Dibujando rutas que ya estaban calculadas...');
        this.dibujarRutaCompleta();
      }
    }, 300 * (intentos + 1)); // Incrementar el tiempo de espera con cada intento
  }

  /**
   * Cargar pedido y calcular rutas
   */
  cargarPedido(id: number): void {
    this.cargando = true;
    this.pedidoService.obtenerPorId(id).subscribe({
      next: (pedido) => {
        this.pedido = pedido;
        this.cargando = false;
         this.cdr.detectChanges();
        
        console.log('📦 Pedido cargado:', pedido);
        console.log('📍 Coordenadas del pedido:', {
          origen: { lat: pedido.latOrigen, lon: pedido.lonOrigen },
          destino: { lat: pedido.latDestino, lon: pedido.lonDestino },
          restaurante: { lat: pedido.latRestaurante, lon: pedido.lonRestaurante }
        });
        console.log('🍽️ Datos del restaurante:', {
  nombre: pedido.nombreRestaurante,
  direccion: pedido.direccionRestaurante,
  latRestaurante: pedido.latRestaurante,
  lonRestaurante: pedido.lonRestaurante,
  tieneRestaurante: !!(pedido.latRestaurante && pedido.lonRestaurante)
});
        
        // Calcular ambas rutas si hay coordenadas
if (this.tieneCoordenadasCompletas()) {
  console.log('✅ Pedido tiene coordenadas completas, calculando rutas...');
  // ✅ Envolver en setTimeout para evitar NG0100
  setTimeout(() => {
    this.calcularTodasLasRutas();
  }, 0);
}else {
          console.warn('⚠️ El pedido no tiene todas las coordenadas necesarias');
          console.log('🔍 Verificación de coordenadas:', {
            tieneOrigen: !!(pedido.latOrigen && pedido.lonOrigen),
            tieneDestino: !!(pedido.latDestino && pedido.lonDestino),
            tieneRestaurante: !!(pedido.latRestaurante && pedido.lonRestaurante)
          });
        }
      },
      error: (err) => {
        this.error = 'Error al cargar el pedido';
        this.cargando = false;
        console.error('❌ Error:', err);
      }
    });
  }

  /**
   * Verificar que el pedido tiene todas las coordenadas necesarias
   */
  tieneCoordenadasCompletas(): boolean {
    if (!this.pedido) return false;
    
    // ✅ Si tiene restaurante, necesita las 3 ubicaciones
    if (this.pedido.latRestaurante && this.pedido.lonRestaurante) {
      return !!(
        this.pedido.latOrigen && this.pedido.lonOrigen &&
        this.pedido.latRestaurante && this.pedido.lonRestaurante &&
        this.pedido.latDestino && this.pedido.lonDestino
      );
    }
    
    // ✅ Si NO tiene restaurante, solo necesita origen y destino (ruta directa)
    return !!(
      this.pedido.latOrigen && this.pedido.lonOrigen &&
      this.pedido.latDestino && this.pedido.lonDestino
    );
  }

 /**
 * Calcular ambas rutas (Pickup y Delivery) o ruta directa
 */
calcularTodasLasRutas(): void {
  if (!this.pedido) return;

  this.calculandoRutas = true;

  // ✅ CASO 1: Pedido CON restaurante (2 fases)
  if (this.pedido.latRestaurante && this.pedido.lonRestaurante) {
    console.log('🍽️ Calculando ruta en 2 fases (con restaurante)');
    
    // 1️⃣ Ruta PICKUP: Repartidor → Restaurante
    const requestPickup = {
      latOrigen: this.pedido.latOrigen!,
      lonOrigen: this.pedido.lonOrigen!,
      latDestino: this.pedido.latRestaurante!,
      lonDestino: this.pedido.lonRestaurante!
    };

    // 2️⃣ Ruta DELIVERY: Restaurante → Cliente
    const requestDelivery = {
      latOrigen: this.pedido.latRestaurante!,
      lonOrigen: this.pedido.lonRestaurante!,
      latDestino: this.pedido.latDestino!,
      lonDestino: this.pedido.lonDestino!
    };

    // ✅ NUEVO: Usar forkJoin para esperar ambas rutas
    const pickup$ = this.lugarService.calcularRutaPorCoordenadas(requestPickup);
    const delivery$ = this.lugarService.calcularRutaPorCoordenadas(requestDelivery);

    // Importar forkJoin al inicio del archivo:
    // import { forkJoin } from 'rxjs';
    
    forkJoin({
      pickup: pickup$,
      delivery: delivery$
    }).subscribe({
      next: (resultados) => {
        setTimeout(() => {
          this.rutaPickup = resultados.pickup.ruta || resultados.pickup;
          this.rutaDelivery = resultados.delivery.ruta || resultados.delivery;
          console.log('✅ Ambas rutas calculadas:', { pickup: this.rutaPickup, delivery: this.rutaDelivery });
          this.calculandoRutas = false;
          this.cdr.detectChanges();
          
          // ✅ Esperar a que el mapa esté listo antes de dibujar
          if (this.mapaListo) {
            setTimeout(async () => {
              await this.dibujarRutaCompleta();
            }, 500);
          } else {
            console.log('⏳ Rutas calculadas, esperando a que el mapa esté listo...');
          }
        }, 0);
      },
      error: (err) => {
        console.error('❌ Error al calcular rutas:', err);
        this.calculandoRutas = false;
      }
    });
  } 
  // ✅ CASO 2: Pedido SIN restaurante (ruta directa)
  else {
    console.log('🍽️ Calculando ruta directa (sin restaurante)');
    
    const requestDirecta = {
      latOrigen: this.pedido.latOrigen!,
      lonOrigen: this.pedido.lonOrigen!,
      latDestino: this.pedido.latDestino!,
      lonDestino: this.pedido.lonDestino!
    };

    // Solo calcular la ruta directa y mostrarla como "delivery"
    this.lugarService.calcularRutaPorCoordenadas(requestDirecta).subscribe({
      next: async (response: any) => {
        setTimeout(() => {
          this.rutaDelivery = response.ruta || response;
          console.log('✅ Ruta directa calculada:', this.rutaDelivery);
          this.calculandoRutas = false;
          this.cdr.detectChanges();
          
          // ✅ Esperar a que el mapa esté listo antes de dibujar
          if (this.mapaListo) {
            setTimeout(async () => {
              await this.dibujarRutaCompleta();
            }, 500);
          } else {
            console.log('⏳ Ruta calculada, esperando a que el mapa esté listo...');
          }
        }, 0);
      },
      error: (err) => {
        console.error('❌ Error al calcular ruta:', err);
        this.calculandoRutas = false;
      }
    });
  }
}

/**
 * ✅ CORREGIDO: Dibujar ruta completa en el mapa usando las rutas calculadas
 */
async dibujarRutaCompleta(): Promise<void> {
  // ✅ Verificar que el mapa esté listo
  if (!this.mapaListo) {
    console.warn('⏳ Mapa no está listo todavía, esperando...');
    // Reintentar después de 500ms
    setTimeout(() => this.dibujarRutaCompleta(), 500);
    return;
  }

  if (!this.mapaComponent) {
    console.warn('⚠️ Componente del mapa no existe');
    return;
  }

  // Verificar que el mapa interno esté inicializado
  const map = (this.mapaComponent as any).map;
  if (!map) {
    console.warn('⏳ Mapa interno no está inicializado, esperando...');
    setTimeout(() => this.dibujarRutaCompleta(), 500);
    return;
  }

  try {
    console.log('🗺️ Iniciando dibujo de rutas...');

    // 🚀 CASO 1: Pedido CON restaurante (2 rutas: Pickup + Delivery)
    if (this.rutaPickup && this.rutaDelivery) {
      console.log('📦 Dibujando ambas rutas (Pickup + Delivery)');
      
      // Dibujar ruta PICKUP usando OSRM
      await this.dibujarRutaConOSRM(
        this.rutaPickup,
        true  // esPickup = true
      );
      
      // Dibujar ruta DELIVERY usando OSRM
      await this.dibujarRutaConOSRM(
        this.rutaDelivery,
        false // esPickup = false
      );
    }
    // 🚀 CASO 2: Pedido SIN restaurante (1 ruta directa)
    else if (this.rutaDelivery) {
      console.log('🚚 Dibujando ruta directa (sin restaurante)');
      
      await this.dibujarRutaConOSRM(
        this.rutaDelivery,
        false // Ruta directa se trata como delivery
      );
    }
    
    console.log('✅ Rutas dibujadas correctamente');
    
  } catch (error) {
    console.error('❌ Error al dibujar rutas:', error);
  }
}

/**
 * 🆕 Dibujar una ruta individual usando OSRM (como lo hace selector-ubicacion)
 */
private async dibujarRutaConOSRM(ruta: any, esPickup: boolean): Promise<void> {
  if (!ruta || !ruta.nodos || ruta.nodos.length < 2) {
    console.warn('⚠️ Ruta sin nodos suficientes:', ruta);
    return;
  }

  const primerNodo = ruta.nodos[0];
  const ultimoNodo = ruta.nodos[ruta.nodos.length - 1];

  const origen = { lat: primerNodo.latitud, lng: primerNodo.longitud };
  const destino = { lat: ultimoNodo.latitud, lng: ultimoNodo.longitud };

  console.log(`🛣️ Obteniendo ruta OSRM (${esPickup ? 'PICKUP' : 'DELIVERY'}):`, { origen, destino });

  // Obtener coordenadas reales de OSRM
  const coordenadas = await this.obtenerRutaOSRM(origen, destino);

  // Determinar color y estilo
  const color = esPickup ? '#3b82f6' : '#10b981';
  const label = esPickup ? '📦 PICKUP' : '🚚 DELIVERY';
  const dashArray = esPickup ? '10, 5' : undefined;

  // Crear polyline en el mapa del componente hijo
  const L = (window as any).L;
  if (!L) {
    console.error('❌ Leaflet no está disponible');
    return;
  }

  const map = (this.mapaComponent as any).map;
  if (!map) {
    console.error('❌ Mapa no está inicializado');
    return;
  }

  // 🆕 Si es la primera ruta (PICKUP o ruta directa), limpiar rutas anteriores
 // 🆕 Solo limpiar si vamos a redibujar desde cero
// NO limpiar entre PICKUP y DELIVERY
if (esPickup && !this.rutaDelivery) {
  // Solo limpiar al dibujar PICKUP si no hay DELIVERY todavía
  this.limpiarRutasDelMapa();
} else if (!esPickup && !this.rutaPickup) {
  // Solo limpiar al dibujar ruta directa (sin restaurante)
  this.limpiarRutasDelMapa();
}

  const polyline = L.polyline(coordenadas, {
    color: color,
    weight: 6,
    opacity: 0.8,
    dashArray: dashArray
  }).addTo(map);

  polyline.bindPopup(`
    <div style="text-align: center;">
      <strong>${label}</strong><br>
      <small>📏 ${this.formatearDistancia(ruta.distanciaTotalKm || 0)}</small><br>
      <small>⏱️ ${ruta.tiempoEstimadoMinutos || 0} min</small>
    </div>
  `);

  // Ajustar vista del mapa
  map.fitBounds(polyline.getBounds(), { padding: [50, 50] });

  console.log(`✅ Ruta ${label} dibujada con ${coordenadas.length} puntos`);
}

/**
 * 🆕 Obtener ruta real de OSRM (igual que selector-ubicacion)
 */
private async obtenerRutaOSRM(origen: { lat: number, lng: number }, destino: { lat: number, lng: number }): Promise<any[]> {
  const url = `https://router.project-osrm.org/route/v1/driving/${origen.lng},${origen.lat};${destino.lng},${destino.lat}?overview=full&geometries=geojson`;
  
  try {
    const response = await fetch(url);
    const data = await response.json();
    
    if (data.routes && data.routes.length > 0) {
      // OSRM devuelve [lng, lat], Leaflet necesita [lat, lng]
      return data.routes[0].geometry.coordinates.map((coord: number[]) => 
        [coord[1], coord[0]]
      );
    }
  } catch (error) {
    console.error('❌ Error obteniendo ruta OSRM:', error);
  }
  
  // Fallback: línea recta
  return [[origen.lat, origen.lng], [destino.lat, destino.lng]];
}


  /**
   * Formatear distancia
   */
  private formatearDistancia(km: number): string {
    return km < 1 ? `${(km * 1000).toFixed(0)}m` : `${km.toFixed(2)}km`;
  }

  /**
   * Volver a la lista de pedidos
   */
  volver(): void {
    this.router.navigate(['/dashboard/pedidos/listar-pedidos']);
  }

  /**
   * Cambiar estado del pedido
   */
  cambiarEstado(nuevoEstado: string): void {
    if (!this.pedido?.id) return;

    this.pedidoService.actualizarEstado(this.pedido.id, nuevoEstado).subscribe({
        next: (pedidoActualizado) => {
        this.pedido = pedidoActualizado;
        this.cdr.detectChanges(); // ✅ Forzar detección
        console.log('✅ Estado actualizado:', pedidoActualizado);
                
        if (nuevoEstado === 'ENTREGADO') {
          setTimeout(() => this.volver(), 2000);
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
      case EstadoPedido.PENDIENTE: return 'badge-warning';
      case EstadoPedido.ASIGNADO: return 'badge-info';
      case EstadoPedido.EN_CAMINO: return 'badge-primary';
      case EstadoPedido.ENTREGADO: return 'badge-success';
      case EstadoPedido.CANCELADO: return 'badge-danger';
      default: return 'badge-secondary';
    }
  }

  /**
   * Obtiene el ícono según el estado
   */
  getEstadoIcono(estado?: EstadoPedido): string {
    switch (estado) {
      case EstadoPedido.PENDIENTE: return '⏳';
      case EstadoPedido.ASIGNADO: return '📋';
      case EstadoPedido.EN_CAMINO: return '🚴';
      case EstadoPedido.ENTREGADO: return '✅';
      case EstadoPedido.CANCELADO: return '❌';
      default: return '📦';
    }
  }

  /**
   * Verifica si se puede cambiar a EN_CAMINO
   */
  puedeMarcarEnCamino(): boolean {
    return this.pedido?.estado === EstadoPedido.ASIGNADO;
  }

  /**
   * Verifica si se puede cambiar a ENTREGADO
   */
  puedeMarcarEntregado(): boolean {
    return this.pedido?.estado === EstadoPedido.EN_CAMINO;
  }

  /**
   * Obtener el tiempo total estimado
   */
  getTiempoTotal(): number {
    const tiempoPickup = this.rutaPickup?.tiempoEstimadoMinutos || 0;
    const tiempoDelivery = this.rutaDelivery?.tiempoEstimadoMinutos || 0;
    return tiempoPickup + tiempoDelivery;
  }

  /**
   * Obtener la distancia total
   */
  getDistanciaTotal(): number {
    const distanciaPickup = this.rutaPickup?.distanciaTotalKm || 0;
    const distanciaDelivery = this.rutaDelivery?.distanciaTotalKm || 0;
    return distanciaPickup + distanciaDelivery;
  }
}