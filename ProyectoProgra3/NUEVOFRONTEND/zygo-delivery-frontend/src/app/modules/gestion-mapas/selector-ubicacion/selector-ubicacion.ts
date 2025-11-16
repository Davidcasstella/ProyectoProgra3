// src/app/modules/gestion-mapas/selector-ubicacion/selector-ubicacion.ts

import { Component, OnInit, OnDestroy, ChangeDetectorRef, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as L from 'leaflet';
import { LugarService } from '../../../services/lugar.service';
import { RutaOptima, Coordenadas } from '../../../models/ruta.model';

@Component({
  selector: 'app-selector-ubicacion',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './selector-ubicacion.html',
  styleUrls: ['./selector-ubicacion.css']
})
export class SelectorUbicacionComponent implements OnInit, OnDestroy {
  
  // 🆕 NUEVAS PROPIEDADES PARA MODO SIMPLE
  @Input() modoSeleccionSimple: boolean = false; // Si true, solo permite seleccionar un punto
  @Output() ubicacionSeleccionada = new EventEmitter<Coordenadas>(); // Emite coordenada seleccionada
  
  // Mapa de Leaflet
  private map!: L.Map;
  
  // Marcadores
  private marcadorOrigen?: L.Marker;
  private marcadorDestino?: L.Marker;
  
  // Polyline de la ruta
  private polylineRuta?: L.Polyline;
  
  // Estado
  origenSeleccionado: Coordenadas | null = null;
  destinoSeleccionado: Coordenadas | null = null;
  rutaCalculada: RutaOptima | null = null;
  calculandoRuta = false;
  errorMensaje = '';
  
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

  constructor(
    private lugarService: LugarService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Pequeño delay para asegurar que el DOM esté listo
    setTimeout(() => {
      this.inicializarMapa();
    }, 100);
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  /**
   * Inicializar el mapa centrado en Sogamoso
   */
  private inicializarMapa(): void {
    // Coordenadas del centro de Sogamoso
    const sogamoso: L.LatLngExpression = [5.7147, -72.9341];

    this.map = L.map('map', {
      center: sogamoso,
      zoom: 14
    });

    // Agregar capa de OpenStreetMap
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);

    // Evento click en el mapa
    this.map.on('click', (e: L.LeafletMouseEvent) => {
      this.alClickarMapa(e);
    });
  }

  /**
   * Manejar clicks en el mapa
   */
  private alClickarMapa(e: L.LeafletMouseEvent): void {
    console.log('🖱️ Click detectado en mapa:', e.latlng);
    
    const coords: Coordenadas = {
      lat: e.latlng.lat,
      lng: e.latlng.lng
    };

    // 🆕 MODO SELECCIÓN SIMPLE (para el modal de crear pedidos)
    if (this.modoSeleccionSimple) {
      console.log('📍 Modo simple - Seleccionando ubicación...');
      
      // Limpiar marcador previo
      if (this.marcadorOrigen) {
        this.marcadorOrigen.remove();
      }
      
      // Colocar nuevo marcador
      this.marcadorOrigen = L.marker([coords.lat, coords.lng], {
        icon: this.iconoOrigen
      }).addTo(this.map)
        .bindPopup('📍 Ubicación seleccionada')
        .openPopup();

      // Emitir el evento con la coordenada
      console.log('✅ Emitiendo ubicación:', coords);
      this.ubicacionSeleccionada.emit(coords);
      return;
    }

    // MODO NORMAL (origen y destino para calcular rutas)
    // Si no hay origen, colocarlo
    if (!this.origenSeleccionado) {
      console.log('📍 Estableciendo origen...');
      this.establecerOrigen(coords);
    } 
    // Si hay origen pero no destino, colocar destino
    else if (!this.destinoSeleccionado) {
      console.log('🎯 Estableciendo destino...');
      this.establecerDestino(coords);
    } 
    // Si ya hay ambos, reiniciar con nuevo origen
    else {
      console.log('🔄 Reiniciando con nuevo origen...');
      this.limpiarMapa();
      this.establecerOrigen(coords);
    }
    
    // Forzar detección de cambios
    this.cdr.detectChanges();
    
    console.log('Estado actual:', {
      origen: this.origenSeleccionado,
      destino: this.destinoSeleccionado
    });
  }

  /**
   * Establecer punto de origen
   */
  private establecerOrigen(coords: Coordenadas): void {
    console.log('✅ Guardando origen:', coords);
    this.origenSeleccionado = { ...coords }; // Clonar objeto para forzar cambio
    
    if (this.marcadorOrigen) {
      this.marcadorOrigen.remove();
    }

    this.marcadorOrigen = L.marker([coords.lat, coords.lng], {
      icon: this.iconoOrigen,
      draggable: true
    })
      .addTo(this.map)
      .bindPopup('📍 <b>Origen</b>')
      .openPopup();

    // Permitir arrastrar el marcador
    this.marcadorOrigen.on('dragend', (e: any) => {
      const newPos = e.target.getLatLng();
      this.origenSeleccionado = { lat: newPos.lat, lng: newPos.lng };
      this.limpiarRuta();
      this.cdr.detectChanges();
    });
    
    console.log('✅ Origen establecido:', this.origenSeleccionado);
  }

  /**
   * Establecer punto de destino
   */
  private establecerDestino(coords: Coordenadas): void {
    console.log('✅ Guardando destino:', coords);
    this.destinoSeleccionado = { ...coords }; // Clonar objeto para forzar cambio
    
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

    // Permitir arrastrar el marcador
    this.marcadorDestino.on('dragend', (e: any) => {
      const newPos = e.target.getLatLng();
      this.destinoSeleccionado = { lat: newPos.lat, lng: newPos.lng };
      this.limpiarRuta();
      this.cdr.detectChanges();
    });
    
    console.log('✅ Destino establecido:', this.destinoSeleccionado);
  }

  /**
   * Calcular ruta óptima
   */
  calcularRuta(): void {
    console.log('🔍 Botón calcular ruta presionado');
    console.log('📍 Origen:', this.origenSeleccionado);
    console.log('🎯 Destino:', this.destinoSeleccionado);

    if (!this.origenSeleccionado || !this.destinoSeleccionado) {
      this.errorMensaje = '⚠️ Debes seleccionar origen y destino en el mapa';
      console.error('❌ Faltan puntos por seleccionar');
      return;
    }

    this.calculandoRuta = true;
    this.errorMensaje = '';

    console.log('🚀 Enviando petición al backend...');

    this.lugarService.calcularRutaPorCoordenadas({
      latOrigen: this.origenSeleccionado.lat,
      lonOrigen: this.origenSeleccionado.lng,
      latDestino: this.destinoSeleccionado.lat,
      lonDestino: this.destinoSeleccionado.lng
    }).subscribe({
      next: (response) => {
        console.log('✅ Ruta recibida:', response);
        this.rutaCalculada = response.ruta;
        this.dibujarRuta(response.ruta);
        this.calculandoRuta = false;
      },
      error: (error) => {
        console.error('❌ Error calculando ruta:', error);
        this.errorMensaje = '❌ Error al calcular la ruta: ' + (error.error?.error || error.message);
        this.calculandoRuta = false;
      }
    });
  }

  /**
   * Dibujar la ruta en el mapa
   */
  public dibujarRuta(ruta: RutaOptima): void {
    // Limpiar ruta anterior
    if (this.polylineRuta) {
      this.polylineRuta.remove();
    }

    // Convertir nodos a coordenadas
    const coordenadas: L.LatLngExpression[] = ruta.nodos.map(nodo => 
      [nodo.latitud, nodo.longitud] as L.LatLngExpression
    );

    // Dibujar polyline
    this.polylineRuta = L.polyline(coordenadas, {
      color: '#3b82f6',
      weight: 5,
      opacity: 0.8
    }).addTo(this.map);

    // Ajustar vista al bounds de la ruta
    this.map.fitBounds(this.polylineRuta.getBounds(), {
      padding: [50, 50]
    });
  }

  /**
   * Limpiar ruta dibujada
   */
  private limpiarRuta(): void {
    if (this.polylineRuta) {
      this.polylineRuta.remove();
      this.polylineRuta = undefined;
    }
    this.rutaCalculada = null;
  }

  /**
   * Limpiar todo el mapa
   */
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

  /**
   * Formatear distancia
   */
  formatearDistancia(km: number): string {
    return km < 1 ? `${(km * 1000).toFixed(0)}m` : `${km.toFixed(2)}km`;
  }

  /**
   * Formatear costo
   */
  formatearCosto(costo: number): string {
    return `$${costo.toLocaleString('es-CO')}`;
  }
}