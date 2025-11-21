// src/app/services/historial-ruta.service.ts

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface HistorialRutaDTO {
  id: number;
  pedidoId: number;
  tipoCalculo: string;
  fechaCalculo: string;
  distanciaTotalKm: number;
  tiempoEstimadoMin: number;
  restauranteId?: number;
  restauranteNombre?: string;
  repartidorId?: number;
  repartidorNombre?: string;
  nodoClienteId?: number;
  nodoRepartidorId?: number;
  nodosRutaJson?: string;
  segmentosRutaJson?: string;
  instruccionesJson?: string;
}

@Injectable({
  providedIn: 'root'
})
export class HistorialRutaService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/historial-rutas`;

  obtenerUltimas(cantidad: number = 10): Observable<HistorialRutaDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/ultimas/${cantidad}`).pipe(
      map((response: any) => {
        if (response && response.historial) {
          return response.historial;
        }
        if (Array.isArray(response)) {
          return response;
        }
        return [];
      })
    );
  }

  obtenerPorPedido(pedidoId: number): Observable<HistorialRutaDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/pedido/${pedidoId}`).pipe(
      map((response: any) => {
        if (response && response.historial) {
          return response.historial;
        }
        if (Array.isArray(response)) {
          return response;
        }
        return [];
      })
    );
  }

  obtenerPorId(id: number): Observable<HistorialRutaDTO> {
    return this.http.get<HistorialRutaDTO>(`${this.apiUrl}/${id}`);
  }

  obtenerPorRestaurante(restauranteId: number): Observable<HistorialRutaDTO[]> {
    return this.http.get<HistorialRutaDTO[]>(`${this.apiUrl}/restaurante/${restauranteId}`);
  }

  obtenerPorRepartidor(repartidorId: number): Observable<HistorialRutaDTO[]> {
    return this.http.get<HistorialRutaDTO[]>(`${this.apiUrl}/repartidor/${repartidorId}`);
  }
}