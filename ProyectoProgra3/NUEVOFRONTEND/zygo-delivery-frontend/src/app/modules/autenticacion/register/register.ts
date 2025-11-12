// src/app/modules/autenticacion/register/register.ts

import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { RegisterRequest, TipoUsuario } from '../../../models/usuario.model';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './register.html',
  styleUrls: ['./register.css']
})
export class RegisterComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  nombre = signal('');
  email = signal('');
  password = signal('');
  confirmPassword = signal('');
  telefono = signal('');
  direccion = signal('');
  tipo = signal<TipoUsuario>(TipoUsuario.CLIENTE);
  
  loading = signal(false);
  error = signal('');

  TipoUsuario = TipoUsuario;

  onRegister(): void {
    // Validaciones
    if (!this.nombre() || !this.email() || !this.password() || !this.telefono() || !this.direccion()) {
      this.error.set('Por favor complete todos los campos');
      return;
    }

    if (this.password() !== this.confirmPassword()) {
      this.error.set('Las contraseñas no coinciden');
      return;
    }

    if (this.password().length < 6) {
      this.error.set('La contraseña debe tener al menos 6 caracteres');
      return;
    }

    this.loading.set(true);
    this.error.set('');

    const registerData: RegisterRequest = {
      nombre: this.nombre(),
      email: this.email(),
      password: this.password(),
      telefono: this.telefono(),
      direccion: this.direccion(),
      tipo: this.tipo()
    };

    this.authService.register(registerData).subscribe({
      next: (response) => {
        console.log('Registro exitoso:', response);
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        console.error('Error en registro:', error);
        this.loading.set(false);
        this.error.set(error.error || 'Error al registrar usuario');
      }
    });
  }

  onBack(): void {
    this.router.navigate(['/login']);
  }
}