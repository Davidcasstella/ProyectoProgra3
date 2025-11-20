// src/app/services/lugar.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap, shareReplay } from 'rxjs/operators';

interface RutaCache {
  datos: any;
  timestamp: number;
}

@Injectable({
  providedIn: 'root'
})
export class LugarService {
  private apiUrl = 'http://localhost:8080/api/rutas';
  private lugaresApiUrl = 'http://localhost:8080/api/lugares'; // ✅ NUEVA URL
  
  // 🗺️ Cache para el grafo completo
  private grafoCache$: Observable<any> | null = null;
  
  // 🚀 Cache para rutas calculadas (key: "lat1,lon1-lat2,lon2")
  private rutasCache = new Map<string, RutaCache>();
  
  // ⏰ Tiempo de vida del cache (5 minutos)
  private readonly CACHE_TTL = 5 * 60 * 1000;

  constructor(private http: HttpClient) {
    console.log('🚀 LugarService inicializado con cache');
  }

  /**
   * ✅ Obtener nodo más cercano (con cache)
   */
  obtenerNodoCercano(lat: number, lon: number): Observable<any> {
    const cacheKey = `nodo-${lat.toFixed(6)}-${lon.toFixed(6)}`;
    
    // Verificar si está en cache y es válido
    const cached = this.rutasCache.get(cacheKey);
    if (cached && this.isCacheValid(cached.timestamp)) {
      console.log('✅ Usando nodo desde cache');
      return of(cached.datos);
    }

    console.log('🔄 Obteniendo nodo cercano desde servidor...');
    
    // Si no está en cache, hacer petición
    return this.http.get<any>(`${this.apiUrl}/nodo-cercano`, {
      params: { lat: lat.toString(), lon: lon.toString() }
    }).pipe(
      tap(datos => {
        this.rutasCache.set(cacheKey, {
          datos,
          timestamp: Date.now()
        });
        console.log('💾 Nodo guardado en cache');
      })
    );
  }

  /**
   * ✅ Calcular ruta óptima por coordenadas (MÉTODO ORIGINAL - Mantiene compatibilidad)
   */
  calcularRutaPorCoordenadas(request: {
    latOrigen: number;
    lonOrigen: number;
    latDestino: number;
    lonDestino: number;
  }): Observable<any> {
    // Crear key única para esta ruta
    const cacheKey = `ruta-${request.latOrigen.toFixed(6)},${request.lonOrigen.toFixed(6)}-${request.latDestino.toFixed(6)},${request.lonDestino.toFixed(6)}`;
    
    // Verificar si está en cache y es válido
    const cached = this.rutasCache.get(cacheKey);
    if (cached && this.isCacheValid(cached.timestamp)) {
      console.log('✅ Usando ruta desde cache:', cacheKey);
      return of(cached.datos);
    }

    console.log('🔄 Calculando nueva ruta desde el servidor...');
    console.log('📍 Origen:', request.latOrigen, request.lonOrigen);
    console.log('🎯 Destino:', request.latDestino, request.lonDestino);

    // Si no está en cache, calcular
    return this.http.post<any>(`${this.apiUrl}/optima`, request).pipe(
      tap(datos => {
        // Guardar en cache
        this.rutasCache.set(cacheKey, {
          datos,
          timestamp: Date.now()
        });
        console.log('💾 Ruta guardada en cache');
        console.log('✅ Ruta calculada exitosamente');
      })
    );
  }

  /**
   * ✅ Calcular ruta óptima (MÉTODO NUEVO - Alias del anterior)
   */
  calcularRutaOptima(
    latOrigen: number, 
    lonOrigen: number, 
    latDestino: number, 
    lonDestino: number
  ): Observable<any> {
    return this.calcularRutaPorCoordenadas({
      latOrigen,
      lonOrigen,
      latDestino,
      lonDestino
    });
  }

  /**
   * ✅ Cargar grafo completo (con cache persistente)
   * El grafo se carga una sola vez por sesión
   */
  cargarGrafo(): Observable<any> {
    if (!this.grafoCache$) {
      console.log('🔄 Cargando grafo desde el servidor...');
      this.grafoCache$ = this.http.get<any>(`${this.apiUrl}/grafo`).pipe(
        tap(() => console.log('✅ Grafo cargado y cacheado')),
        shareReplay(1) // Cachea el resultado y lo comparte entre suscriptores
      );
    } else {
      console.log('✅ Usando grafo desde cache');
    }
    
    return this.grafoCache$;
  }

  /**
   * 🧹 Limpiar cache manualmente (útil cuando se actualiza el mapa)
   */
  limpiarCache(): void {
    console.log('🧹 Limpiando cache...');
    this.rutasCache.clear();
    this.grafoCache$ = null;
  }

  /**
   * 🗑️ Limpiar cache expirado automáticamente
   */
  limpiarCacheExpirado(): void {
    const ahora = Date.now();
    let eliminados = 0;

    this.rutasCache.forEach((cache, key) => {
      if (!this.isCacheValid(cache.timestamp)) {
        this.rutasCache.delete(key);
        eliminados++;
      }
    });

    if (eliminados > 0) {
      console.log(`🗑️ ${eliminados} entradas de cache eliminadas por expiración`);
    }
  }

  /**
   * ✅ Verificar si el cache es válido
   */
  private isCacheValid(timestamp: number): boolean {
    return Date.now() - timestamp < this.CACHE_TTL;
  }

  /**
   * 📊 Obtener estadísticas del cache
   */
  obtenerEstadisticasCache(): { 
    totalEntradas: number; 
    entradasValidas: number; 
    entradasExpiradas: number;
    memoriaUsadaKB: number;
  } {
    const ahora = Date.now();
    let validas = 0;
    let expiradas = 0;

    this.rutasCache.forEach(cache => {
      if (this.isCacheValid(cache.timestamp)) {
        validas++;
      } else {
        expiradas++;
      }
    });

    // Estimación aproximada de memoria usada
    const memoriaUsadaKB = Math.round((this.rutasCache.size * 2) / 1024);

    return {
      totalEntradas: this.rutasCache.size,
      entradasValidas: validas,
      entradasExpiradas: expiradas,
      memoriaUsadaKB
    };
  }

  /**
   * 🔄 Pre-cargar rutas comunes (opcional)
   * Útil para pre-cachear rutas que se usan frecuentemente
   */
  precargarRutasComunes(rutas: Array<{
    latOrigen: number;
    lonOrigen: number;
    latDestino: number;
    lonDestino: number;
  }>): void {
    console.log(`🚀 Pre-cargando ${rutas.length} rutas comunes...`);
    
    rutas.forEach((ruta, index) => {
      setTimeout(() => {
        this.calcularRutaPorCoordenadas(ruta).subscribe({
          next: () => console.log(`✅ Ruta ${index + 1}/${rutas.length} pre-cargada`),
          error: (err) => console.error(`❌ Error pre-cargando ruta ${index + 1}:`, err)
        });
      }, index * 500); // Espaciadas 500ms para no saturar el servidor
    });
  }

  // ✅ CORREGIDO: Obtener restaurantes con la URL correcta
  obtenerRestaurantes(): Observable<any[]> {
    console.log('🍽️ Llamando a:', `${this.lugaresApiUrl}/restaurantes`);
    return this.http.get<any[]>(`${this.lugaresApiUrl}/restaurantes`).pipe(
      tap(data => console.log('✅ Restaurantes recibidos del backend:', data))
    );
  }
}