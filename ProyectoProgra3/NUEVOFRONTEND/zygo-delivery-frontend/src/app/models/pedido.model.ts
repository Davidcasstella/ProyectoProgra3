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
  
  // ✨ NUEVOS CAMPOS OPCIONALES para el mapa
  // (solo en frontend por ahora, no se envían al backend)
  latOrigen?: number;
  lonOrigen?: number;
  latDestino?: number;
  lonDestino?: number;
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

/**
 * DTO para crear un pedido con información del mapa
 * (usa las coordenadas para calcular la ruta antes de crear el pedido)
 */
export interface CrearPedidoConMapa {
  clienteId: number;
  descripcion: string;
  direccionOrigen: string;
  direccionDestino: string;
  latOrigen: number;
  lonOrigen: number;
  latDestino: number;
  lonDestino: number;
}