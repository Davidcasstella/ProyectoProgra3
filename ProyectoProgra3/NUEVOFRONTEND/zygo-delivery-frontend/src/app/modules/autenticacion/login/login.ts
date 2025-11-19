// src/app/modules/autenticacion/login/login.ts

import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { LoginRequest } from '../../../models/usuario.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  email = signal('');
  password = signal('');
  loading = signal(false);
  error = signal('');

  onLogin(): void {
    if (!this.email() || !this.password()) {
      this.error.set('Por favor ingrese email y contraseña');
      return;
    }

    this.loading.set(true);
    this.error.set('');

    const credentials: LoginRequest = {
      email: this.email(),
      password: this.password()
    };

    this.authService.login(credentials).subscribe({
      next: (response) => {
        console.log('✅ Login exitoso:', response);
        this.loading.set(false);
        
        // ✨ NUEVO - Redirigir según el ROL del usuario
        const returnUrl = this.route.snapshot.queryParams['returnUrl'];
        
        if (returnUrl) {
          // Si hay returnUrl, ir ahí
          this.router.navigate([returnUrl]);
        } else {
          // Si no, redirigir según el rol
          this.redirigirSegunRol(response.tipoUsuario);
        }
      },
      error: (error) => {
        console.error('❌ Error en login:', error);
        this.loading.set(false);
        this.error.set(error.error?.message || 'Email o contraseña incorrectos');
      }
    });
  }

  /**
   * ✨ NUEVO - Redirige al usuario según su rol
   */
  private redirigirSegunRol(rol: string): void {
    console.log('🎯 Redirigiendo según rol:', rol);

    switch (rol) {
      case 'ADMIN':
        console.log('🔴 Acceso ADMIN - Redirigiendo a dashboard completo');
        this.router.navigate(['/dashboard']);
        break;

      case 'CLIENTE':
        console.log('🟢 Acceso CLIENTE - Redirigiendo a mis pedidos');
        this.router.navigate(['/dashboard']); // O '/pedidos/mis-pedidos' si tienes una ruta específica
        break;

      case 'REPARTIDOR':
        console.log('🟡 Acceso REPARTIDOR - Redirigiendo a mis entregas');
        this.router.navigate(['/dashboard']); // O '/repartidor/mis-entregas' si tienes una ruta específica
        break;

      default:
        console.warn('⚠️ Rol desconocido:', rol);
        this.router.navigate(['/dashboard']);
    }
  }

  onRegister(): void {
    this.router.navigate(['/register']);
  }
}