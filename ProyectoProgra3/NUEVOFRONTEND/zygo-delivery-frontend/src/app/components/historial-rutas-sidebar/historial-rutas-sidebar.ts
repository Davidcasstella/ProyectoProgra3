import { Component, OnInit, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HistorialRutaService, HistorialRutaDTO } from '../../services/historial-ruta.service';

@Component({
  selector: 'app-historial-rutas-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './historial-rutas-sidebar.html',
  styleUrls: ['./historial-rutas-sidebar.css']
})
export class HistorialRutasSidebarComponent implements OnInit {
  @Output() rutaSeleccionada = new EventEmitter<HistorialRutaDTO>();
  
  rutas: HistorialRutaDTO[] = [];
  cargando = true;
  error = '';
  
  tipoFiltro: 'todas' | 'pickup' | 'delivery' = 'todas';

  constructor(
    private historialService: HistorialRutaService,
    private cdr: ChangeDetectorRef  // 🆕 Agregar esto
  ) {}

  ngOnInit(): void {
    this.cargarHistorial();
    
    setInterval(() => {
      this.cargarHistorial();
    }, 30000);
  }

  cargarHistorial(): void {
    this.cargando = true;
    this.error = '';
    
    this.historialService.obtenerUltimas(20).subscribe({
      next: (rutas) => {
        console.log('✅ Historial cargado:', rutas);
        this.rutas = rutas || [];
        this.cargando = false;
        this.cdr.detectChanges();  // 🆕 Forzar actualización
      },
      error: (err) => {
        console.error('❌ Error cargando historial:', err);
        this.error = 'Error al cargar el historial de rutas';
        this.cargando = false;
        this.cdr.detectChanges();  // 🆕 Forzar actualización
      }
    });
  }

  get rutasFiltradas(): HistorialRutaDTO[] {
    if (this.tipoFiltro === 'todas') {
      return this.rutas;
    }
    const tipoCalculo = this.tipoFiltro === 'pickup' ? 'RUTA_PICKUP' : 'RUTA_DELIVERY';
    return this.rutas.filter(r => r.tipoCalculo === tipoCalculo);
  }

  seleccionarRuta(ruta: HistorialRutaDTO): void {
    console.log('📍 Ruta seleccionada:', ruta);
    this.rutaSeleccionada.emit(ruta);
  }

  obtenerIconoTipo(tipo: string): string {
    switch (tipo) {
      case 'RUTA_PICKUP': return '📦';
      case 'RUTA_DELIVERY': return '🚚';
      default: return '🗺️';
    }
  }

  obtenerColorTipo(tipo: string): string {
    switch (tipo) {
      case 'RUTA_PICKUP': return '#3b82f6';
      case 'RUTA_DELIVERY': return '#10b981';
      default: return '#6b7280';
    }
  }

  formatearFecha(fecha: string): string {
    const date = new Date(fecha);
    const ahora = new Date();
    const diffMs = ahora.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    
    if (diffMins < 1) return 'Ahora mismo';
    if (diffMins < 60) return `Hace ${diffMins} min`;
    if (diffMins < 1440) return `Hace ${Math.floor(diffMins / 60)} h`;
    return date.toLocaleDateString('es-CO');
  }

  formatearDistancia(km: number): string {
    return km < 1 ? `${(km * 1000).toFixed(0)}m` : `${km.toFixed(2)}km`;
  }
}