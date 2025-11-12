// src/app/models/pedido.model.ts

export enum EstadoPedido {
  PENDIENTE = 'PENDIENTE',
  ASIGNADO = 'ASIGNADO',
  EN_CAMINO = 'EN_CAMINO',
  ENTREGADO = 'ENTREGADO',
  CANCELADO = 'CANCELADO'
}

export interface Pedido {
  id?: number;
  clienteId: number;
  repartidorId?: number;
  descripcion: string;
  direccionOrigen: string;
  direccionDestino: string;
  distanciaKm: number;
  costo?: number;
  estado?: EstadoPedido;
  fechaCreacion?: string;
  fechaAsignacion?: string;
  fechaEntrega?: string;
  nombreCliente?: string;
  nombreRepartidor?: string;
}

export interface PedidoDTO {
  id?: number;
  clienteId: number;
  repartidorId?: number;
  descripcion: string;
  direccionOrigen: string;
  direccionDestino: string;
  distanciaKm: number;
  costo?: number;
  estado?: EstadoPedido;
  fechaCreacion?: string;
  fechaAsignacion?: string;
  fechaEntrega?: string;
  nombreCliente?: string;
  nombreRepartidor?: string;
}