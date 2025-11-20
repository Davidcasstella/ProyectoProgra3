// src/app/modules/gestion-mapas/selector-ubicacion/selector-ubicacion.ts

import { Component, OnInit, OnDestroy, ChangeDetectorRef, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import * as L from 'leaflet';
import { LugarService } from '../../../services/lugar.service';
import { UsuarioService } from '../../../services/usuario.service'; // ✅ NUEVO
import { AuthService } from '../../../services/auth.service'; // ✅ NUEVO
import { RutaOptima, Coordenadas } from '../../../models/ruta.model';
import { FormsModule } from '@angular/forms'; // ✅ AGREGAR ESTE IMPORT

@Component({
  selector: 'app-selector-ubicacion',
  standalone: true,
  
  imports: [CommonModule, FormsModule], // ✅ AGREGAR FormsModule AQUÍ
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
  private marcadoresRestaurantes: L.Marker[] = []; // ✅ NUEVO
  private marcadoresRepartidores: L.Marker[] = []; // ✅ NUEVO
  
  // Polyline de la ruta
  private polylineRuta?: L.Polyline;
  
  // Estado
  origenSeleccionado: Coordenadas | null = null;
  destinoSeleccionado: Coordenadas | null = null;
  rutaCalculada: RutaOptima | null = null;
  calculandoRuta = false;
  errorMensaje = '';
  
  // ✅ NUEVO: Control de visibilidad de capas
  mostrarRestaurantes = true;
  mostrarRepartidores = true;
  esAdmin = false;
  
  // ✅ NUEVO: Datos
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
  
  // ✅ NUEVO: Iconos para restaurantes y repartidores
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
    private usuarioService: UsuarioService, // ✅ NUEVO
    private authService: AuthService, // ✅ NUEVO
    private cdr: ChangeDetectorRef,
    private router: Router
  ) {}

  ngOnInit(): void {
    // ✅ NUEVO: Verificar si es admin
    this.verificarUsuario();
    
    setTimeout(() => {
      this.inicializarMapa();
      
      // ✅ NUEVO: Cargar restaurantes y repartidores si es admin
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
  
  // ✅ NUEVO: Verificar tipo de usuario
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
  console.log('🔗 URL del servicio:', this.lugarService); // ✅ NUEVO LOG
  
  this.lugarService.obtenerRestaurantes().subscribe({
    next: (restaurantes) => {
      console.log('✅ Restaurantes recibidos:', restaurantes);
      console.log('📊 Cantidad:', restaurantes.length); // ✅ NUEVO LOG
      this.restaurantes = restaurantes;
      
      // ✅ FORZAR LA VISUALIZACIÓN
      setTimeout(() => {
        this.mostrarMarcadoresRestaurantes();
      }, 500);
    },
    error: (error) => {
      console.error('❌ Error cargando restaurantes:', error);
      console.error('❌ Detalles del error:', error.message); // ✅ NUEVO LOG
      console.error('❌ Status:', error.status); // ✅ NUEVO LOG
    }
  });
}

  
  // ✅ NUEVO: Cargar repartidores desde el backend
  private cargarRepartidores(): void {
    console.log('🚴 Cargando repartidores...');
    
    this.usuarioService.obtenerRepartidores().subscribe({
      next: (repartidores) => {
        console.log('✅ Repartidores recibidos:', repartidores);
        this.repartidores = repartidores.filter(r => r.latitud && r.longitud);
        this.mostrarMarcadoresRepartidores();
      },
      error: (error) => {
        console.error('❌ Error cargando repartidores:', error);
      }
    });
  }
  
  // ✅ NUEVO: Mostrar marcadores de restaurantes en el mapa
  private mostrarMarcadoresRestaurantes(): void {
    // Limpiar marcadores previos
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
  
  // ✅ NUEVO: Mostrar marcadores de repartidores en el mapa
  private mostrarMarcadoresRepartidores(): void {
    // Limpiar marcadores previos
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
  
  // ✅ NUEVO: Toggle visibilidad de restaurantes
  toggleRestaurantes(): void {
    this.mostrarRestaurantes = !this.mostrarRestaurantes;
    
    if (this.mostrarRestaurantes) {
      this.mostrarMarcadoresRestaurantes();
    } else {
      this.marcadoresRestaurantes.forEach(m => m.remove());
      this.marcadoresRestaurantes = [];
    }
  }
  
  // ✅ NUEVO: Toggle visibilidad de repartidores
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
      console.log('📍 Modo simple - Seleccionando ubicación...');
      
      if (this.marcadorOrigen) {
        this.marcadorOrigen.remove();
      }
      
      this.marcadorOrigen = L.marker([coords.lat, coords.lng], {
        icon: this.iconoOrigen
      }).addTo(this.map)
        .bindPopup('📍 Ubicación seleccionada')
        .openPopup();

      console.log('✅ Emitiendo ubicación:', coords);
      this.ubicacionSeleccionada.emit(coords);
      return;
    }

    // MODO NORMAL (origen y destino para calcular rutas)
    if (!this.origenSeleccionado) {
      console.log('🟢 Estableciendo origen...');
      this.establecerOrigen(coords);
    } 
    else if (!this.destinoSeleccionado) {
      console.log('🎯 Estableciendo destino...');
      this.establecerDestino(coords);
    } 
    else {
      console.log('🔄 Reiniciando con nuevo origen...');
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
    this.origenSeleccionado = null;
    this.destinoSeleccionado = null;
    this.errorMensaje = '';
  }

  formatearDistancia(km: number): string {
    return km < 1 ? `${(km * 1000).toFixed(0)}m` : `${km.toFixed(2)}km`;
  }

  formatearCosto(costo: number): string {
    return `$${costo.toLocaleString('es-CO')}`;
  }
}