import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Ubicacion {
  latitud: number;
  longitud: number;
}

export interface UbicacionRespuesta {
  mensaje: string;
  latitud: number;
  longitud: number;
  nombreCliente: string;
}

export interface MisPedidos {
  id: number;
  descripcion: string;
  direccionOrigen: string;
  direccionDestino: string;
  distanciaKm: number;
  costo: number;
  estado: string;
  fechaCreacion: string;
  nombreRepartidor?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ClienteService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/cliente`;
  
  // 📍 Observable para compartir ubicación entre componentes
  private ubicacionSubject = new BehaviorSubject<Ubicacion | null>(null);
  public ubicacion$ = this.ubicacionSubject.asObservable();

  constructor() {
    // Cargar ubicación guardada al iniciar
    this.obtenerMiUbicacion().subscribe(
      (ubicacion) => {
        this.ubicacionSubject.next({
          latitud: ubicacion.latitud,
          longitud: ubicacion.longitud
        });
      },
      () => {
        console.log('📍 Sin ubicación registrada aún');
      }
    );
  }

  /**
   * 💾 Guardar mi ubicación en el servidor
   */
  guardarMiUbicacion(latitud: number, longitud: number): Observable<UbicacionRespuesta> {
    const request = { latitud, longitud };
    return this.http.post<UbicacionRespuesta>(
      `${this.apiUrl}/guardar-ubicacion`,
      request
    );
  }

  /**
   * 📍 Obtener mi ubicación guardada
   */
  obtenerMiUbicacion(): Observable<any> {
    return this.http.get(`${this.apiUrl}/mi-ubicacion`);
  }

  /**
   * 📦 Obtener mis pedidos
   */
  obtenerMisPedidos(): Observable<MisPedidos[]> {
    return this.http.get<MisPedidos[]>(`${this.apiUrl}/mis-pedidos`);
  }

  /**
   * 📦 Obtener un pedido específico mío
   */
  obtenerMiPedido(id: number): Observable<MisPedidos> {
    return this.http.get<MisPedidos>(`${this.apiUrl}/mis-pedidos/${id}`);
  }

  /**
   * ➕ Crear un nuevo pedido
   */
  crearPedido(pedido: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/crear-pedido`, pedido);
  }

  /**
   * ❌ Cancelar un pedido (solo si está PENDIENTE)
   */
  cancelarPedido(id: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/mis-pedidos/${id}/cancelar`, {});
  }

  /**
   * 📊 Obtener mis estadísticas
   */
  obtenerMisEstadisticas(): Observable<any> {
    return this.http.get(`${this.apiUrl}/mis-estadisticas`);
  }

  /**
   * 📍 Actualizar ubicación en el observable (uso local)
   */
  actualizarUbicacionLocal(ubicacion: Ubicacion): void {
    this.ubicacionSubject.next(ubicacion);
  }

  /**
   * 📍 Obtener ubicación actual del observable
   */
  obtenerUbicacionLocal(): Ubicacion | null {
    return this.ubicacionSubject.value;
  }
  // src/app/services/cliente.service.ts

guardarUbicacion(latitud: number, longitud: number): Observable<any> {
  return this.http.put(`${this.apiUrl}/guardar-ubicacion`, null, {
    params: { latitud: latitud.toString(), longitud: longitud.toString() }
  });
}

obtenerPerfil(): Observable<any> {
  return this.http.get(`${this.apiUrl}/mi-perfil`);
}
}