// src/app/modules/gestion-mapas/selector-ubicacion/selector-ubicacion.ts

import { Component, OnInit, OnDestroy, ChangeDetectorRef, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import * as L from 'leaflet';
import { LugarService } from '../../../services/lugar.service';
import { UsuarioService } from '../../../services/usuario.service';
import { AuthService } from '../../../services/auth.service';
import { HistorialRutaService, HistorialRutaDTO } from '../../../services/historial-ruta.service';
import { RutaOptima, Coordenadas } from '../../../models/ruta.model';
import { FormsModule } from '@angular/forms';
import { HistorialRutasSidebarComponent } from '../../../components/historial-rutas-sidebar/historial-rutas-sidebar';

@Component({
  selector: 'app-selector-ubicacion',
  standalone: true,
  imports: [CommonModule, FormsModule, HistorialRutasSidebarComponent],
  templateUrl: './selector-ubicacion.html',
  styleUrls: ['./selector-ubicacion.css']
})
export class SelectorUbicacionComponent implements OnInit, OnDestroy {
  
  // PROPIEDADES PARA MODO SIMPLE
  @Input() modoSeleccionSimple: boolean = false;
  @Output() ubicacionSeleccionada = new EventEmitter<Coordenadas>();
  
  // Mapa de Leaflet
  private map!: L.Map;
  
  // Marcadores
  private marcadorOrigen?: L.Marker;
  private marcadorDestino?: L.Marker;
  private marcadoresRestaurantes: L.Marker[] = [];
  private marcadoresRepartidores: L.Marker[] = [];
  private marcadoresPuntosClave: L.Marker[] = [];
  
  // Polylines de rutas
  private polylineRuta?: L.Polyline;
  private polylinePickup?: L.Polyline; // 🆕 Ruta PICKUP
  private polylineDelivery?: L.Polyline; // 🆕 Ruta DELIVERY
  
  // Estado
  origenSeleccionado: Coordenadas | null = null;
  destinoSeleccionado: Coordenadas | null = null;
  rutaCalculada: RutaOptima | null = null;
  calculandoRuta = false;
  errorMensaje = '';
  
  // 🆕 Estado del historial
  rutaHistorialActiva: HistorialRutaDTO | null = null;
  
  // Control de visibilidad de capas
  mostrarRestaurantes = true;
  mostrarRepartidores = true;
  esAdmin = false;
  
  // Datos
  restaurantes: any[] = [];
  repartidores: any[] = [];
  
  // Iconos personalizados
  private iconoOrigen = L.icon({
    iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowSize: [41, 41]
  });
  
  private iconoDestino = L.icon({
    iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowSize: [41, 41]
  });
  
  private iconoRestaurante = L.icon({
    iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-orange.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowSize: [41, 41]
  });
  
  private iconoRepartidor = L.icon({
    iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-blue.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowSize: [41, 41]
  });

  constructor(
    private lugarService: LugarService,
    private usuarioService: UsuarioService,
    private authService: AuthService,
    private historialService: HistorialRutaService, // 🆕
    private cdr: ChangeDetectorRef,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.verificarUsuario();
    
    setTimeout(() => {
      this.inicializarMapa();
      
      if (this.esAdmin) {
        this.cargarRestaurantes();
        this.cargarRepartidores();
      }
    }, 100);
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }
  
  private verificarUsuario(): void {
    const usuario = this.authService.getUsuarioActual();
    this.esAdmin = usuario?.tipo === 'ADMIN';
    console.log('👤 Usuario actual:', usuario);
    console.log('🔑 Es admin:', this.esAdmin);
  }

  volverAtras(): void {
    console.log('⬅️ Volviendo al dashboard...');
    this.router.navigate(['/dashboard']);
  }

  private inicializarMapa(): void {
    const sogamoso: L.LatLngExpression = [5.7147, -72.9341];

    this.map = L.map('map', {
      center: sogamoso,
      zoom: 14
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);

    this.map.on('click', (e: L.LeafletMouseEvent) => {
      this.alClickarMapa(e);
    });
  }
  
private cargarRestaurantes(): void {
  console.log('🍽️ Cargando restaurantes...');
  
  this.lugarService.obtenerRestaurantes().subscribe({
    next: (restaurantes) => {
      console.log('✅ Restaurantes recibidos:', restaurantes);
      this.restaurantes = restaurantes;
      // ✅ Usar detectChanges en lugar de setTimeout
      this.cdr.detectChanges();
      this.mostrarMarcadoresRestaurantes();
    },
    error: (error) => {
      console.error('❌ Error cargando restaurantes:', error);
    }
  });
}
  private cargarRepartidores(): void {
  console.log('🚴 Cargando repartidores...');
  
  this.usuarioService.obtenerRepartidores().subscribe({
    next: (repartidores) => {
      console.log('✅ Repartidores recibidos:', repartidores);
      this.repartidores = repartidores.filter(r => r.latitud && r.longitud);
      // ✅ FIX: Usar detectChanges para evitar ExpressionChangedAfterItHasBeenCheckedError
      this.cdr.detectChanges();
      this.mostrarMarcadoresRepartidores();
    },
    error: (error) => {
      console.error('❌ Error cargando repartidores:', error);
    }
  });
}
  
  private mostrarMarcadoresRestaurantes(): void {
    this.marcadoresRestaurantes.forEach(m => m.remove());
    this.marcadoresRestaurantes = [];
    
    if (!this.mostrarRestaurantes) return;
    
    this.restaurantes.forEach(restaurante => {
      const marcador = L.marker(
        [restaurante.latitud, restaurante.longitud],
        { icon: this.iconoRestaurante }
      )
      .addTo(this.map)
      .bindPopup(`
        <div style="text-align: center;">
          <strong>🍽️ ${restaurante.nombre}</strong><br>
          <small>${restaurante.direccion || 'Sin dirección'}</small><br>
          <small style="color: #666;">Lat: ${restaurante.latitud.toFixed(6)}, Lng: ${restaurante.longitud.toFixed(6)}</small>
        </div>
      `);
      
      this.marcadoresRestaurantes.push(marcador);
    });
    
    console.log(`✅ ${this.marcadoresRestaurantes.length} restaurantes mostrados en el mapa`);
  }
  
  private mostrarMarcadoresRepartidores(): void {
    this.marcadoresRepartidores.forEach(m => m.remove());
    this.marcadoresRepartidores = [];
    
    if (!this.mostrarRepartidores) return;
    
    this.repartidores.forEach(repartidor => {
      const disponible = repartidor.disponible ? '✅ Disponible' : '❌ Ocupado';
      const color = repartidor.disponible ? '#10b981' : '#ef4444';
      
      const marcador = L.marker(
        [repartidor.latitud, repartidor.longitud],
        { icon: this.iconoRepartidor }
      )
      .addTo(this.map)
      .bindPopup(`
        <div style="text-align: center;">
          <strong>🚴 ${repartidor.nombre}</strong><br>
          <span style="color: ${color}; font-weight: bold;">${disponible}</span><br>
          <small style="color: #666;">Tel: ${repartidor.telefono || 'N/A'}</small><br>
          <small style="color: #666;">Lat: ${repartidor.latitud.toFixed(6)}, Lng: ${repartidor.longitud.toFixed(6)}</small>
        </div>
      `);
      
      this.marcadoresRepartidores.push(marcador);
    });
    
    console.log(`✅ ${this.marcadoresRepartidores.length} repartidores mostrados en el mapa`);
  }
  
  toggleRestaurantes(): void {
    this.mostrarRestaurantes = !this.mostrarRestaurantes;
    
    if (this.mostrarRestaurantes) {
      this.mostrarMarcadoresRestaurantes();
    } else {
      this.marcadoresRestaurantes.forEach(m => m.remove());
      this.marcadoresRestaurantes = [];
    }
  }
  
  toggleRepartidores(): void {
    this.mostrarRepartidores = !this.mostrarRepartidores;
    
    if (this.mostrarRepartidores) {
      this.mostrarMarcadoresRepartidores();
    } else {
      this.marcadoresRepartidores.forEach(m => m.remove());
      this.marcadoresRepartidores = [];
    }
  }

  private alClickarMapa(e: L.LeafletMouseEvent): void {
    console.log('🖱️ Click detectado en mapa:', e.latlng);
    
    const coords: Coordenadas = {
      lat: e.latlng.lat,
      lng: e.latlng.lng
    };

    if (this.modoSeleccionSimple) {
      if (this.marcadorOrigen) {
        this.marcadorOrigen.remove();
      }
      
      this.marcadorOrigen = L.marker([coords.lat, coords.lng], {
        icon: this.iconoOrigen
      }).addTo(this.map)
        .bindPopup('📍 Ubicación seleccionada')
        .openPopup();

      this.ubicacionSeleccionada.emit(coords);
      return;
    }

    if (!this.origenSeleccionado) {
      this.establecerOrigen(coords);
    } 
    else if (!this.destinoSeleccionado) {
      this.establecerDestino(coords);
    } 
    else {
      this.limpiarMapa();
      this.establecerOrigen(coords);
    }
    
    this.cdr.detectChanges();
  }

  private establecerOrigen(coords: Coordenadas): void {
    this.origenSeleccionado = { ...coords };
    
    if (this.marcadorOrigen) {
      this.marcadorOrigen.remove();
    }

    this.marcadorOrigen = L.marker([coords.lat, coords.lng], {
      icon: this.iconoOrigen,
      draggable: true
    })
      .addTo(this.map)
      .bindPopup('🟢 <b>Origen</b>')
      .openPopup();

    this.marcadorOrigen.on('dragend', (e: any) => {
      const newPos = e.target.getLatLng();
      this.origenSeleccionado = { lat: newPos.lat, lng: newPos.lng };
      this.limpiarRuta();
      this.cdr.detectChanges();
    });
    
    if (!this.modoSeleccionSimple) {
      this.ubicacionSeleccionada.emit(this.origenSeleccionado);
    }
  }

  private establecerDestino(coords: Coordenadas): void {
    this.destinoSeleccionado = { ...coords };
    
    if (this.marcadorDestino) {
      this.marcadorDestino.remove();
    }

    this.marcadorDestino = L.marker([coords.lat, coords.lng], {
      icon: this.iconoDestino,
      draggable: true
    })
      .addTo(this.map)
      .bindPopup('🎯 <b>Destino</b>')
      .openPopup();

    this.marcadorDestino.on('dragend', (e: any) => {
      const newPos = e.target.getLatLng();
      this.destinoSeleccionado = { lat: newPos.lat, lng: newPos.lng };
      this.limpiarRuta();
      this.cdr.detectChanges();
    });
    
    if (!this.modoSeleccionSimple) {
      this.ubicacionSeleccionada.emit(this.destinoSeleccionado);
    }
  }

  calcularRuta(): void {
    if (!this.origenSeleccionado || !this.destinoSeleccionado) {
      this.errorMensaje = '⚠️ Debes seleccionar origen y destino en el mapa';
      return;
    }

    this.calculandoRuta = true;
    this.errorMensaje = '';

    this.lugarService.calcularRutaPorCoordenadas({
      latOrigen: this.origenSeleccionado.lat,
      lonOrigen: this.origenSeleccionado.lng,
      latDestino: this.destinoSeleccionado.lat,
      lonDestino: this.destinoSeleccionado.lng
    }).subscribe({
      next: (response) => {
        this.rutaCalculada = response.ruta;
        this.dibujarRuta(response.ruta);
        this.calculandoRuta = false;
      },
      error: (error) => {
        this.errorMensaje = '❌ Error al calcular la ruta: ' + (error.error?.error || error.message);
        this.calculandoRuta = false;
      }
    });
  }

  public dibujarRuta(ruta: RutaOptima): void {
    if (this.polylineRuta) {
      this.polylineRuta.remove();
    }

    const coordenadas: L.LatLngExpression[] = ruta.nodos.map(nodo => 
      [nodo.latitud, nodo.longitud] as L.LatLngExpression
    );

    this.polylineRuta = L.polyline(coordenadas, {
      color: '#3b82f6',
      weight: 5,
      opacity: 0.8
    }).addTo(this.map);

    this.map.fitBounds(this.polylineRuta.getBounds(), {
      padding: [50, 50]
    });
  }

visualizarRutaHistorial(ruta: HistorialRutaDTO): void {
  console.log('🎨 Visualizando ruta del historial:', ruta);
  
  this.limpiarRutasHistorial();
  this.rutaHistorialActiva = ruta;
  
  this.historialService.obtenerPorPedido(ruta.pedidoId).subscribe({
    next: async (response: any) => {
      const rutas = response.historial || response;
      
      // Usar OSRM para cada ruta
      for (const r of rutas) {
        await this.dibujarRutaHistorialConOSRM(r);
      }
      
      this.ajustarVistaParaRutas();
    },
    error: (err) => {
      console.error('❌ Error:', err);
      this.dibujarRutaHistorialConOSRM(ruta);
    }
  });
}

// 🆕 Ajustar vista para mostrar todas las rutas
private ajustarVistaParaRutas(): void {
  const bounds = L.latLngBounds([]);
  
  if (this.polylinePickup) {
    bounds.extend(this.polylinePickup.getBounds());
  }
  if (this.polylineDelivery) {
    bounds.extend(this.polylineDelivery.getBounds());
  }
  
  if (bounds.isValid()) {
    this.map.fitBounds(bounds, { padding: [50, 50] });
  }
}

 // 🆕 Dibujar ruta del historial con marcadores
private dibujarRutaHistorial(ruta: HistorialRutaDTO): void {
  // ✅ FIX: Usar nodosRutaJson
  let nodos: any = ruta.nodosRutaJson;
  
  if (typeof nodos === 'string') {
    try {
      nodos = JSON.parse(nodos);
    } catch (e) {
      console.error('Error parseando nodos:', e);
      return;
    }
  }

  if (!nodos || nodos.length === 0) {
    console.warn('⚠️ No hay nodos para dibujar');
    return;
  }

  console.log('📍 Nodos a dibujar:', nodos.length);

  // Convertir nodos a coordenadas
  const coordenadas: L.LatLngExpression[] = nodos.map((nodo: any) => 
    [nodo.latitud, nodo.longitud] as L.LatLngExpression
  );

  // Determinar color según tipo de ruta
  const esPickup = ruta.tipoCalculo === 'RUTA_PICKUP';
  const color = esPickup ? '#3b82f6' : '#10b981';
  const label = esPickup ? '📦 PICKUP' : '🚚 DELIVERY';

  // Crear polyline
  const polyline = L.polyline(coordenadas, {
    color: color,
    weight: 6,
    opacity: 0.8,
    dashArray: esPickup ? '10, 5' : undefined
  }).addTo(this.map);

  // Guardar referencia
  if (esPickup) {
    this.polylinePickup = polyline;
  } else {
    this.polylineDelivery = polyline;
  }

  // Agregar popup a la línea
  polyline.bindPopup(`
    <div style="text-align: center;">
      <strong>${label}</strong><br>
      <small>📏 ${this.formatearDistancia(ruta.distanciaTotalKm)}</small><br>
      <small>⏱️ ${ruta.tiempoEstimadoMin} min</small>
    </div>
  `);

  // 🆕 AGREGAR MARCADORES DE PUNTOS CLAVE
  this.agregarMarcadoresPuntosClave(ruta, nodos, esPickup);

  // Ajustar vista del mapa
  this.map.fitBounds(polyline.getBounds(), {
    padding: [50, 50]
  });

  console.log(`✅ Ruta ${label} dibujada con ${nodos.length} nodos`);
}

// 🆕 Marcadores para puntos clave


// 🆕 Agregar marcadores de puntos clave (Restaurante, Repartidor, Cliente)
private agregarMarcadoresPuntosClave(ruta: HistorialRutaDTO, nodos: any[], esPickup: boolean): void {
  
  // Icono personalizado para el restaurante (naranja)
  const iconoRestauranteMarcador = L.divIcon({
    className: 'custom-marker',
    html: `<div style="
      background: #f97316;
      color: white;
      width: 36px;
      height: 36px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      border: 3px solid white;
      box-shadow: 0 2px 6px rgba(0,0,0,0.3);
    ">🍽️</div>`,
    iconSize: [36, 36],
    iconAnchor: [18, 18]
  });

  // Icono para el repartidor (azul)
  const iconoRepartidorMarcador = L.divIcon({
    className: 'custom-marker',
    html: `<div style="
      background: #3b82f6;
      color: white;
      width: 36px;
      height: 36px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      border: 3px solid white;
      box-shadow: 0 2px 6px rgba(0,0,0,0.3);
    ">🚴</div>`,
    iconSize: [36, 36],
    iconAnchor: [18, 18]
  });

  // Icono para el cliente (verde)
  const iconoClienteMarcador = L.divIcon({
    className: 'custom-marker',
    html: `<div style="
      background: #10b981;
      color: white;
      width: 36px;
      height: 36px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      border: 3px solid white;
      box-shadow: 0 2px 6px rgba(0,0,0,0.3);
    ">👤</div>`,
    iconSize: [36, 36],
    iconAnchor: [18, 18]
  });

  // El primer nodo es el origen, el último es el destino
  const primerNodo = nodos[0];
  const ultimoNodo = nodos[nodos.length - 1];

  if (esPickup) {
    // PICKUP: Repartidor → Restaurante
    // Primer nodo = Repartidor, Último nodo = Restaurante
    
    // Marcador Repartidor (inicio)
    const marcadorRepartidor = L.marker(
      [primerNodo.latitud, primerNodo.longitud],
      { icon: iconoRepartidorMarcador }
    ).addTo(this.map)
     .bindPopup(`
       <div style="text-align: center;">
         <strong>🚴 Repartidor</strong><br>
         <small>${ruta.repartidorNombre || 'Repartidor asignado'}</small><br>
         <small style="color: #3b82f6;">📍 Punto de inicio</small>
       </div>
     `);
    this.marcadoresPuntosClave.push(marcadorRepartidor);

    // Marcador Restaurante (fin del pickup)
    const marcadorRestaurante = L.marker(
      [ultimoNodo.latitud, ultimoNodo.longitud],
      { icon: iconoRestauranteMarcador }
    ).addTo(this.map)
     .bindPopup(`
       <div style="text-align: center;">
         <strong>🍽️ Restaurante</strong><br>
         <small>${ruta.restauranteNombre || 'Restaurante'}</small><br>
         <small style="color: #f97316;">📦 Recoger pedido aquí</small>
       </div>
     `);
    this.marcadoresPuntosClave.push(marcadorRestaurante);

  } else {
    // DELIVERY: Restaurante → Cliente
    // Primer nodo = Restaurante, Último nodo = Cliente
    
    // Marcador Restaurante (inicio del delivery)
    const marcadorRestaurante = L.marker(
      [primerNodo.latitud, primerNodo.longitud],
      { icon: iconoRestauranteMarcador }
    ).addTo(this.map)
     .bindPopup(`
       <div style="text-align: center;">
         <strong>🍽️ Restaurante</strong><br>
         <small>${ruta.restauranteNombre || 'Restaurante'}</small><br>
         <small style="color: #f97316;">📦 Salida del pedido</small>
       </div>
     `);
    this.marcadoresPuntosClave.push(marcadorRestaurante);

    // Marcador Cliente (fin)
    const marcadorCliente = L.marker(
      [ultimoNodo.latitud, ultimoNodo.longitud],
      { icon: iconoClienteMarcador }
    ).addTo(this.map)
     .bindPopup(`
       <div style="text-align: center;">
         <strong>👤 Cliente</strong><br>
         <small>Destino de entrega</small><br>
         <small style="color: #10b981;">🎯 Entregar aquí</small>
       </div>
     `);
    this.marcadoresPuntosClave.push(marcadorCliente);
  }
}



  // 🆕 Cerrar visualización de ruta del historial
  cerrarRutaHistorial(): void {
    this.limpiarRutasHistorial();
    this.rutaHistorialActiva = null;
  }

 // 🆕 Limpiar rutas del historial
private limpiarRutasHistorial(): void {
  if (this.polylinePickup) {
    this.polylinePickup.remove();
    this.polylinePickup = undefined;
  }
  
  if (this.polylineDelivery) {
    this.polylineDelivery.remove();
    this.polylineDelivery = undefined;
  }
  
  // 🆕 Limpiar marcadores de puntos clave
  this.marcadoresPuntosClave.forEach(m => m.remove());
  this.marcadoresPuntosClave = [];
}
  private limpiarRuta(): void {
    if (this.polylineRuta) {
      this.polylineRuta.remove();
      this.polylineRuta = undefined;
    }
    this.rutaCalculada = null;
  }

  limpiarMapa(): void {
    if (this.marcadorOrigen) {
      this.marcadorOrigen.remove();
      this.marcadorOrigen = undefined;
    }
    if (this.marcadorDestino) {
      this.marcadorDestino.remove();
      this.marcadorDestino = undefined;
    }
    this.limpiarRuta();
    this.limpiarRutasHistorial();
    this.origenSeleccionado = null;
    this.destinoSeleccionado = null;
    this.errorMensaje = '';
    this.rutaHistorialActiva = null;
  }

  formatearDistancia(km: number): string {
    return km < 1 ? `${(km * 1000).toFixed(0)}m` : `${km.toFixed(2)}km`;
  }

  formatearCosto(costo: number): string {
    return `$${costo.toLocaleString('es-CO')}`;
  }
  // Agregar este método para obtener ruta real de OSRM
private async obtenerRutaOSRM(origen: Coordenadas, destino: Coordenadas): Promise<L.LatLngExpression[]> {
  const url = `https://router.project-osrm.org/route/v1/driving/${origen.lng},${origen.lat};${destino.lng},${destino.lat}?overview=full&geometries=geojson`;
  
  try {
    const response = await fetch(url);
    const data = await response.json();
    
    if (data.routes && data.routes.length > 0) {
      // OSRM devuelve [lng, lat], Leaflet necesita [lat, lng]
      return data.routes[0].geometry.coordinates.map((coord: number[]) => 
        [coord[1], coord[0]] as L.LatLngExpression
      );
    }
  } catch (error) {
    console.error('Error obteniendo ruta OSRM:', error);
  }
  
  // Fallback: línea recta
  return [[origen.lat, origen.lng], [destino.lat, destino.lng]];
}

// Modificar dibujarRutaHistorial para usar OSRM
private async dibujarRutaHistorialConOSRM(ruta: HistorialRutaDTO): Promise<void> {
  let nodos: any = ruta.nodosRutaJson;
  
  if (typeof nodos === 'string') {
    try {
      nodos = JSON.parse(nodos);
    } catch (e) {
      console.error('Error parseando nodos:', e);
      return;
    }
  }

  if (!nodos || nodos.length < 2) {
    console.warn('⚠️ No hay suficientes nodos');
    return;
  }

  // Obtener solo origen y destino
  const primerNodo = nodos[0];
  const ultimoNodo = nodos[nodos.length - 1];
  
  const origen: Coordenadas = { lat: primerNodo.latitud, lng: primerNodo.longitud };
  const destino: Coordenadas = { lat: ultimoNodo.latitud, lng: ultimoNodo.longitud };

  // 🚀 Obtener ruta REAL de OSRM
  const coordenadas = await this.obtenerRutaOSRM(origen, destino);

  const esPickup = ruta.tipoCalculo === 'RUTA_PICKUP';
  const color = esPickup ? '#3b82f6' : '#10b981';
  const label = esPickup ? '📦 PICKUP' : '🚚 DELIVERY';

  const polyline = L.polyline(coordenadas, {
    color: color,
    weight: 6,
    opacity: 0.8,
    dashArray: esPickup ? '10, 5' : undefined
  }).addTo(this.map);

  if (esPickup) {
    this.polylinePickup = polyline;
  } else {
    this.polylineDelivery = polyline;
  }

  polyline.bindPopup(`
    <div style="text-align: center;">
      <strong>${label}</strong><br>
      <small>📍 ${this.formatearDistancia(ruta.distanciaTotalKm)}</small><br>
      <small>⏱️ ${ruta.tiempoEstimadoMin} min</small>
    </div>
  `);

  this.agregarMarcadoresPuntosClave(ruta, nodos, esPickup);
  this.map.fitBounds(polyline.getBounds(), { padding: [50, 50] });
}
}