// src/app/models/usuario.model.ts

export enum TipoUsuario {
  CLIENTE = 'CLIENTE',
  REPARTIDOR = 'REPARTIDOR',
  ADMIN = 'ADMIN'
}

// Modelo principal de Usuario
export interface Usuario {
  id?: number;
  nombre: string;
  email: string;
  telefono: string;
  direccion: string;
  tipo: TipoUsuario;
  fechaRegistro?: Date;
  activo?: boolean;
}

// ===== INTERFACES PARA JWT =====

// Request para login
export interface LoginRequest {
  email: string;
  password: string;
}

// Request para registro
export interface RegisterRequest {
  nombre: string;
  email: string;
  password: string;
  telefono: string;
  direccion: string;
  tipo: TipoUsuario;
}

// Response del backend al hacer login/register
export interface LoginResponse {
  token: string;
  tipo: string;
  id: number;
  nombre: string;
  email: string;
  tipoUsuario: TipoUsuario;
}

// Usuario autenticado (lo que guardamos en el frontend)
export interface AuthUser {
  id: number;
  nombre: string;
  email: string;
  tipo: TipoUsuario;  // Cambiado de tipoUsuario a tipo para coincidir con el HTML
  tipoUsuario: TipoUsuario;
  token: string;
}

// DTO para transferencia de datos (usado en servicios)
export interface UsuarioDTO {
  id?: number;
  nombre: string;
  email: string;
  telefono: string;
  direccion: string;
  tipo: TipoUsuario;
  fechaRegistro?: Date;
  activo?: boolean;
  latitud?: number;
  longitud?: number;
  disponible?: boolean;
}
export interface Usuario extends UsuarioDTO {}