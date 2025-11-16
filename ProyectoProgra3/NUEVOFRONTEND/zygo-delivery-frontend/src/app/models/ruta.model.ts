// src/app/models/ruta.model.ts

/**
 * Modelo para nodos (puntos) en una ruta
 */
export interface NodoRuta {
  id: number;
  nombre: string;
  latitud: number;
  longitud: number;
  tipo?: string;
  direccion?: string;
}

/**
 * Modelo para segmentos de la ruta (calles entre nodos)
 */
export interface SegmentoRuta {
  nodoOrigenId: number;
  nodoDestinoId: number;
  distanciaKm: number;
  tiempoEstimadoMinutos: number;
  nombreCalle?: string;
  factorTrafico?: number;
}

/**
 * Modelo principal de la ruta óptima
 */
export interface RutaOptima {
  nodos: NodoRuta[];
  segmentos: SegmentoRuta[];
  distanciaTotalKm: number;
  tiempoEstimadoMinutos: number;
  costoEstimado: number;
  considerandoTrafico?: boolean;
  instrucciones: string[];
}

/**
 * Request para calcular ruta por coordenadas
 */
export interface CalcularRutaCoordenadas {
  latOrigen: number;
  lonOrigen: number;
  latDestino: number;
  lonDestino: number;
}

/**
 * Response del endpoint de calcular ruta
 */
export interface RutaResponse {
  ruta: RutaOptima;
  origen: string;
  destino: string;
}

/**
 * Modelo para lugares/puntos de interés
 */
export interface Lugar {
  id: number;
  nombre: string;
  latitud: number;
  longitud: number;
  tipo: string;
  direccion?: string;
}

/**
 * Coordenadas simples (para marcadores en el mapa)
 */
export interface Coordenadas {
  lat: number;
  lng: number;
}

/**
 * Ubicación con información completa
 */
export interface Ubicacion {
  coordenadas: Coordenadas;
  direccion: string;
  nombre?: string;
}