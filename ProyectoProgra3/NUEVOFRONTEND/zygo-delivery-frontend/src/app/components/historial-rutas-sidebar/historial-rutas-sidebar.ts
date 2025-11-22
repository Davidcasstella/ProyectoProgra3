// src/app/components/historial-rutas-sidebar/historial-rutas-sidebar.ts

import { Component, OnInit, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HistorialRutaService, HistorialRutaDTO } from '../../services/historial-ruta.service';

interface PedidoAgrupado {
  pedidoId: number;
  restauranteNombre: string;
  repartidorNombre: string;
  fechaCalculo: Date;
  rutas: HistorialRutaDTO[];
  distanciaTotal: number;
  tiempoTotal: number;
}

@Component({
  selector: 'app-historial-rutas-sidebar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './historial-rutas-sidebar.html',
  styleUrls: ['./historial-rutas-sidebar.css']
})
export class HistorialRutasSidebarComponent implements OnInit {
  
  @Output() rutaSeleccionada = new EventEmitter<HistorialRutaDTO>();
  
  // Datos
  pedidos: PedidoAgrupado[] = [];
  pedidosFiltrados: PedidoAgrupado[] = [];
  
  // Estados
  cargando = true;
  error = '';
  
  // Filtros
  busqueda = '';
  ordenPor: 'fecha' | 'distancia' | 'tiempo' = 'fecha';

  constructor(
    private historialService: HistorialRutaService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarHistorial();
    
    // Auto-refresh cada 30 segundos
    setInterval(() => {
      this.cargarHistorial();
    }, 30000);
  }

 cargarHistorial(): void {
  this.cargando = true;
  this.error = '';
  
  this.historialService.obtenerUltimas(50).subscribe({
    next: (response: any) => {
      // ✅ Envolver en setTimeout para evitar NG0100
      setTimeout(() => {
        // El servicio puede devolver { historial: [...] } o directamente [...]
        const rutas = response.historial || response || [];
        console.log('✅ Historial cargado:', rutas.length, 'rutas');
        
        // Agrupar rutas por pedido
        this.agruparPorPedido(rutas);
        this.aplicarFiltros();
        
        this.cargando = false;
        this.cdr.detectChanges();
      }, 0);
    },
    error: (err) => {
      setTimeout(() => {
        console.error('❌ Error cargando historial:', err);
        this.error = 'Error al cargar el historial';
        this.cargando = false;
        this.cdr.detectChanges();
      }, 0);
    }
  });
}
  private agruparPorPedido(rutas: HistorialRutaDTO[]): void {
    const pedidosMap = new Map<number, PedidoAgrupado>();
    
    rutas.forEach(ruta => {
      if (!pedidosMap.has(ruta.pedidoId)) {
        pedidosMap.set(ruta.pedidoId, {
          pedidoId: ruta.pedidoId,
          restauranteNombre: ruta.restauranteNombre || 'Sin nombre',
          repartidorNombre: ruta.repartidorNombre || 'Sin asignar',
          fechaCalculo: new Date(ruta.fechaCalculo),
          rutas: [],
          distanciaTotal: 0,
          tiempoTotal: 0
        });
      }
      
      const pedido = pedidosMap.get(ruta.pedidoId)!;
      pedido.rutas.push(ruta);
      pedido.distanciaTotal += ruta.distanciaTotalKm;
      pedido.tiempoTotal += ruta.tiempoEstimadoMin;
    });
    
    this.pedidos = Array.from(pedidosMap.values());
    console.log('📦 Pedidos agrupados:', this.pedidos.length);
  }

  aplicarFiltros(): void {
    let resultado = [...this.pedidos];
    
    // Filtro de búsqueda
    if (this.busqueda.trim()) {
      const termino = this.busqueda.toLowerCase();
      resultado = resultado.filter(p => 
        p.restauranteNombre.toLowerCase().includes(termino) ||
        p.repartidorNombre.toLowerCase().includes(termino) ||
        p.pedidoId.toString().includes(termino)
      );
    }
    
    // Ordenamiento
    resultado.sort((a, b) => {
      switch(this.ordenPor) {
        case 'fecha':
          return b.fechaCalculo.getTime() - a.fechaCalculo.getTime();
        case 'distancia':
          return b.distanciaTotal - a.distanciaTotal;
        case 'tiempo':
          return b.tiempoTotal - a.tiempoTotal;
        default:
          return 0;
      }
    });
    
    this.pedidosFiltrados = resultado;
  }

  seleccionarPedido(pedido: PedidoAgrupado): void {
    console.log('🎯 Pedido seleccionado:', pedido);
    // Emitir la primera ruta del pedido (el componente padre cargará todas)
    if (pedido.rutas.length > 0) {
      this.rutaSeleccionada.emit(pedido.rutas[0]);
    }
  }

  formatearFecha(fecha: Date): string {
    const ahora = new Date();
    const diff = ahora.getTime() - fecha.getTime();
    const minutos = Math.floor(diff / 60000);
    const horas = Math.floor(diff / 3600000);
    const dias = Math.floor(diff / 86400000);
    
    if (minutos < 1) return 'Ahora mismo';
    if (minutos < 60) return `Hace ${minutos} min`;
    if (horas < 24) return `Hace ${horas}h`;
    if (dias < 7) return `Hace ${dias}d`;
    
    return fecha.toLocaleDateString('es-CO', { 
      day: '2-digit', 
      month: 'short',
      year: fecha.getFullYear() !== ahora.getFullYear() ? 'numeric' : undefined
    });
  }

  formatearDistancia(km: number): string {
    return km < 1 ? `${(km * 1000).toFixed(0)}m` : `${km.toFixed(2)}km`;
  }

  limpiarBusqueda(): void {
    this.busqueda = '';
    this.aplicarFiltros();
  }

  cambiarOrden(orden: 'fecha' | 'distancia' | 'tiempo'): void {
    this.ordenPor = orden;
    this.aplicarFiltros();
  }
}