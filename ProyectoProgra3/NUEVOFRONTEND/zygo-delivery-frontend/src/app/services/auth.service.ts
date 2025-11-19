// src/app/services/auth.service.ts

import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { 
  LoginRequest, 
  LoginResponse, 
  RegisterRequest, 
  AuthUser 
} from '../models/usuario.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private apiUrl = `${environment.apiUrl}/auth`;

  private currentUserSubject = new BehaviorSubject<AuthUser | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  // Signals para un manejo más moderno del estado
  isAuthenticated = signal<boolean>(false);
  currentUserSignal = signal<AuthUser | null>(null);

  constructor() {
    this.cargarUsuarioDelStorage();
  }

  // Login con JWT
  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        this.guardarSesion(response);
      })
    );
  }

  // Registro con JWT
  register(data: RegisterRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/register`, data).pipe(
      tap(response => {
        this.guardarSesion(response);
      })
    );
  }

  // Logout
  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('currentUser');
    this.currentUserSubject.next(null);
    this.isAuthenticated.set(false);
    this.currentUserSignal.set(null);
    this.router.navigate(['/login']);
  }

  // Obtener token
  getToken(): string | null {
    return localStorage.getItem('token');
  }

  // Verificar si está autenticado
  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  // Obtener usuario actual
  getCurrentUser(): AuthUser | null {
    return this.currentUserSubject.value;
  }

  // ✨ NUEVO - Obtener ROL del usuario actual
  getUserRole(): string | null {
    const user = this.getCurrentUser();
    return user?.tipoUsuario || user?.tipo || null;
  }

  // ✨ NUEVO - Verificar si es ADMIN
  isAdmin(): boolean {
    return this.getUserRole() === 'ADMIN';
  }

  // ✨ NUEVO - Verificar si es CLIENTE
  isCliente(): boolean {
    return this.getUserRole() === 'CLIENTE';
  }

  // ✨ NUEVO - Verificar si es REPARTIDOR
  isRepartidor(): boolean {
    return this.getUserRole() === 'REPARTIDOR';
  }

  // Guardar sesión
  private guardarSesion(response: LoginResponse): void {
    const user: AuthUser = {
      id: response.id,
      nombre: response.nombre,
      email: response.email,
      tipo: response.tipoUsuario,
      tipoUsuario: response.tipoUsuario,
      token: response.token
    };

    localStorage.setItem('token', response.token);
    localStorage.setItem('currentUser', JSON.stringify(user));
    
    this.currentUserSubject.next(user);
    this.isAuthenticated.set(true);
    this.currentUserSignal.set(user);

    console.log('✅ Sesión guardada - Rol:', user.tipoUsuario);
  }

  // Cargar usuario del storage al iniciar
  private cargarUsuarioDelStorage(): void {
    const token = localStorage.getItem('token');
    const userStr = localStorage.getItem('currentUser');

    if (token && userStr) {
      try {
        const user = JSON.parse(userStr);
        this.currentUserSubject.next(user);
        this.isAuthenticated.set(true);
        this.currentUserSignal.set(user);
        console.log('🔄 Usuario cargado del storage - Rol:', user.tipoUsuario);
      } catch (error) {
        console.error('❌ Error al cargar usuario del storage:', error);
        this.logout();
      }
    }
  }
}