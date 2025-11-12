// src/app/modules/gestion-usuarios/crear-usuarios/crear-usuarios.ts

import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../../components/navbar/navbar';
import { UsuarioService } from '../../../services/usuario.service';
import { UsuarioDTO, TipoUsuario } from '../../../models/usuario.model';

@Component({
  selector: 'app-crear-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './crear-usuarios.html',
  styleUrls: ['./crear-usuarios.css']
})
export class CrearUsuariosComponent {
  private usuarioService = inject(UsuarioService);
  private router = inject(Router);

  TipoUsuario = TipoUsuario;
  
  usuario = signal<UsuarioDTO>({
    nombre: '',
    email: '',
    telefono: '',
    direccion: '',
    tipo: TipoUsuario.CLIENTE,
    activo: true
  });

  loading = signal(false);
  error = signal('');

  onSubmit() {
    this.error.set('');

    // Validaciones
    if (!this.usuario().nombre.trim()) {
      this.error.set('El nombre es obligatorio');
      return;
    }

    if (!this.usuario().email.trim()) {
      this.error.set('El email es obligatorio');
      return;
    }

    if (!this.usuario().telefono.trim()) {
      this.error.set('El teléfono es obligatorio');
      return;
    }

    if (!this.usuario().direccion.trim()) {
      this.error.set('La dirección es obligatoria');
      return;
    }

    this.loading.set(true);

    this.usuarioService.crearUsuario(this.usuario()).subscribe({
      next: (usuarioCreado: UsuarioDTO) => {
        console.log('Usuario creado:', usuarioCreado);
        this.router.navigate(['/usuarios']);
      },
      error: (error: any) => {
        console.error('Error al crear usuario:', error);
        this.error.set(error.error?.message || 'Error al crear el usuario');
        this.loading.set(false);
      }
    });
  }

  updateField(field: keyof UsuarioDTO, value: any) {
    this.usuario.update(u => ({ ...u, [field]: value }));
  }

  cancelar() {
    this.router.navigate(['/usuarios']);
  }
}