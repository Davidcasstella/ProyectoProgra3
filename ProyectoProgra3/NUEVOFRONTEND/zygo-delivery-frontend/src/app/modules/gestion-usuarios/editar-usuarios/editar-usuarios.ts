// src/app/modules/gestion-usuarios/editar-usuarios/editar-usuarios.ts

import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { NavbarComponent } from '../../../components/navbar/navbar';
import { UsuarioService } from '../../../services/usuario.service';
import { UsuarioDTO, TipoUsuario } from '../../../models/usuario.model';

@Component({
  selector: 'app-editar-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './editar-usuarios.html',
  styleUrls: ['./editar-usuarios.css']
})
export class EditarUsuariosComponent implements OnInit {
  private usuarioService = inject(UsuarioService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  TipoUsuario = TipoUsuario;
  usuarioId = signal<number>(0);
  
  usuario = signal<UsuarioDTO>({
    nombre: '',
    email: '',
    telefono: '',
    direccion: '',
    tipo: TipoUsuario.CLIENTE,
    activo: true
  });

  loading = signal(true);
  saving = signal(false);
  error = signal('');

  ngOnInit() {
    const id = this.route.snapshot.params['id'];
    this.usuarioId.set(+id);
    this.cargarUsuario();
  }

  cargarUsuario() {
    this.usuarioService.obtenerUsuarioPorId(this.usuarioId()).subscribe({
      next: (usuario: UsuarioDTO) => {
        this.usuario.set(usuario);
        this.loading.set(false);
      },
      error: (error: any) => {
        console.error('Error al cargar usuario:', error);
        this.error.set('Error al cargar el usuario');
        this.loading.set(false);
      }
    });
  }

  onSubmit() {
    this.error.set('');

    if (!this.usuario().nombre.trim()) {
      this.error.set('El nombre es obligatorio');
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

    this.saving.set(true);

    this.usuarioService.actualizarUsuario(this.usuarioId(), this.usuario()).subscribe({
      next: () => {
        this.router.navigate(['/usuarios']);
      },
      error: (error: any) => {
        console.error('Error al actualizar usuario:', error);
        this.error.set('Error al actualizar el usuario');
        this.saving.set(false);
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