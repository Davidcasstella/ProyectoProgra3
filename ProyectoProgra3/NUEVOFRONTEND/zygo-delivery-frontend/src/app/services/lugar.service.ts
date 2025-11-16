// src/app/services/lugar.service.ts

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { 
  RutaOptima, 
  RutaResponse, 
  CalcularRutaCoordenadas,
  Lugar 
} from '../models/ruta.model';

@Injectable({
  providedIn: 'root'
})
export class LugarService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/lugares`;

  /**
   * Buscar lugares por nombre (para autocompletar)
   */
  buscarLugares(query: string): Observable<any> {
    const params = new HttpParams().set('query', query);
    return this.http.get<any>(`${this.apiUrl}/buscar`, { params });
  }

  /**
   * Listar todos los lugares disponibles
   */
  listarLugares(tipo?: string, limite: number = 50): Observable<any> {
    let params = new HttpParams().set('limite', limite.toString());
    if (tipo) {
      params = params.set('tipo', tipo);
    }
    return this.http.get<any>(`${this.apiUrl}/listar`, { params });
  }

  /**
   * Buscar lugares cercanos a una ubicación
   */
  buscarLugaresCercanos(lat: number, lon: number, radioKm: number = 1.0): Observable<any> {
    const params = new HttpParams()
      .set('lat', lat.toString())
      .set('lon', lon.toString())
      .set('radioKm', radioKm.toString());
    return this.http.get<any>(`${this.apiUrl}/cercanos`, { params });
  }

  /**
   * Calcular ruta óptima usando COORDENADAS (para el mapa interactivo)
   */
  calcularRutaPorCoordenadas(coordenadas: CalcularRutaCoordenadas): Observable<RutaResponse> {
    return this.http.post<RutaResponse>(
      `${this.apiUrl}/calcular-ruta-coordenadas`, 
      coordenadas
    );
  }

  /**
   * Calcular ruta óptima usando NOMBRES de lugares
   */
  calcularRutaPorNombre(origen: string, destino: string, considerarTrafico: boolean = true): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/calcular-ruta`, {
      origen,
      destino,
      considerarTrafico
    });
  }
}