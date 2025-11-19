// src/app/services/pedido.service.ts

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Pedido } from '../models/pedido.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PedidoService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/pedidos`;

  obtenerTodos(): Observable<Pedido[]> {
    // ✅ Agregar timestamp para evitar cache del navegador
    const timestamp = new Date().getTime();
    return this.http.get<Pedido[]>(`${this.apiUrl}?_t=${timestamp}`);
  }

  obtenerPorId(id: number): Observable<Pedido> {
    return this.http.get<Pedido>(`${this.apiUrl}/${id}`);
  }

  crear(pedido: Pedido): Observable<Pedido> {
    return this.http.post<Pedido>(this.apiUrl, pedido);
  }

  actualizar(id: number, pedido: Pedido): Observable<Pedido> {
    return this.http.put<Pedido>(`${this.apiUrl}/${id}`, pedido);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  obtenerPorCliente(clienteId: number): Observable<Pedido[]> {
    const timestamp = new Date().getTime();
    return this.http.get<Pedido[]>(`${this.apiUrl}/cliente/${clienteId}?_t=${timestamp}`);
  }

  obtenerPorRepartidor(repartidorId: number): Observable<Pedido[]> {
    const timestamp = new Date().getTime();
    return this.http.get<Pedido[]>(`${this.apiUrl}/repartidor/${repartidorId}?_t=${timestamp}`);
  }

  asignarRepartidor(pedidoId: number, repartidorId: number): Observable<Pedido> {
    return this.http.put<Pedido>(`${this.apiUrl}/${pedidoId}/asignar/${repartidorId}`, {});
  }

  actualizarEstado(pedidoId: number, estado: string): Observable<Pedido> {
    return this.http.put<Pedido>(`${this.apiUrl}/${pedidoId}/estado?estado=${estado}`, {});
  }

  // 🆕 MÉTODO PARA OBTENER HISTORIAL COMPLETO
  obtenerHistorial(estado?: string): Observable<Pedido[]> {
    const timestamp = new Date().getTime();
    const url = estado 
      ? `${this.apiUrl}/historial?estado=${estado}&_t=${timestamp}`
      : `${this.apiUrl}/historial?_t=${timestamp}`;
    return this.http.get<Pedido[]>(url);
  }

  // 🆕 MÉTODO PARA OBTENER SOLO PEDIDOS ENTREGADOS
  obtenerHistorialEntregados(): Observable<Pedido[]> {
    const timestamp = new Date().getTime();
    return this.http.get<Pedido[]>(`${this.apiUrl}/historial/entregados?_t=${timestamp}`);
  }

  // 🆕 MÉTODO PARA OBTENER SOLO PEDIDOS ACTIVOS (EN PROGRESO)
  obtenerPedidosActivos(): Observable<Pedido[]> {
    const timestamp = new Date().getTime();
    return this.http.get<Pedido[]>(`${this.apiUrl}/historial/activos?_t=${timestamp}`);
  }
}