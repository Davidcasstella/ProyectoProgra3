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
  
  // ✅ Coordenadas de origen (puede ser repartidor o restaurante según el contexto)
  latOrigen?: number;
  lonOrigen?: number;
  
  // ✅ Coordenadas de destino (cliente)
  latDestino?: number;
  lonDestino?: number;
  
  // ✅ NUEVO: Coordenadas del restaurante (punto intermedio)
  latRestaurante?: number;
  lonRestaurante?: number;
  nombreRestaurante?: string;
  direccionRestaurante?: string;
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
  // ✅ NUEVO: Coordenadas opcionales del restaurante
  latRestaurante?: number;
  lonRestaurante?: number;
  restauranteId?: number;
}