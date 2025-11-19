import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * 🚀 Servicio para comunicarse con el backend de rutas
 * Consume el endpoint POST /api/rutas/optima que usa Dijkstra
 */
@Injectable({
  providedIn: 'root'
})
export class RutaService {
  
  private apiUrl = 'http://localhost:8080/api/rutas'; // Ajusta según tu backend

  constructor(private http: HttpClient) { }

  /**
   * 🎯 Calcula ruta óptima entre dos coordenadas usando Dijkstra
   * @param latOrigen Latitud del origen
   * @param lonOrigen Longitud del origen
   * @param latDestino Latitud del destino
   * @param lonDestino Longitud del destino
   * @returns Observable con la ruta completa y todas las coordenadas
   */
  calcularRutaOptima(
    latOrigen: number,
    lonOrigen: number,
    latDestino: number,
    lonDestino: number
  ): Observable<RutaResponse> {
    
    const payload = {
      latOrigen,
      lonOrigen,
      latDestino,
      lonDestino
    };

    console.log('📍 Solicitando ruta óptima:', payload);
    
    return this.http.post<RutaResponse>(`${this.apiUrl}/optima`, payload);
  }

  /**
   * 💾 Health check del servicio de rutas
   */
  healthCheck(): Observable<any> {
    return this.http.get(`${this.apiUrl}/health`);
  }
}

/**
 * 📊 Interface con la estructura de respuesta del backend
 */
export interface RutaResponse {
  origen: string;
  destino: string;
  ruta: RutaOptimaDTO;
  tiempoCalculoMs: number;
  resumen: {
    distanciaTotal: number;
    tiempoEstimado: number;
    numeroNodos: number;
    numeroSegmentos: number;
  };
}

export interface RutaOptimaDTO {
  nodos: NodoDTO[];
  segmentos: SegmentoRuta[];
  distanciaTotalKm: number;
  tiempoEstimadoMinutos: number;
  costoEstimado: number;
  considerandoTrafico: boolean;
  instrucciones: string[];
}

export interface NodoDTO {
  id: number;
  nombre: string;
  latitud: number;
  longitud: number;
  tipo: string;
  direccion: string;
}

export interface SegmentoRuta {
  nodoOrigenId: number;
  nodoDestinoId: number;
  distanciaKm: number;
  tiempoEstimadoMinutos: number;
  nombreCalle: string;
  factorTrafico?: number;
}