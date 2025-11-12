// src/app/modules/gestion-usuarios/listar-usuarios/listar-usuarios.ts

import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../../components/navbar/navbar';
import { UsuarioService } from '../../../services/usuario.service';
import { Usuario, UsuarioDTO, TipoUsuario } from '../../../models/usuario.model';

@Component({
  selector: 'app-listar-usuarios',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './listar-usuarios.html',
  styleUrls: ['./listar-usuarios.css']
})
export class ListarUsuariosComponent implements OnInit {
  private usuarioService = inject(UsuarioService);
  private router = inject(Router);

  usuarios = signal<UsuarioDTO[]>([]);
  usuariosFiltrados = signal<UsuarioDTO[]>([]);
  loading = signal(true);
  filtroTipo = signal<string>('TODOS');
  busqueda = signal('');

  TipoUsuario = TipoUsuario;

  ngOnInit() {
    this.cargarUsuarios();
  }

  cargarUsuarios() {
    this.loading.set(true);
    this.usuarioService.obtenerTodosLosUsuarios().subscribe({
      next: (usuarios: UsuarioDTO[]) => {
        this.usuarios.set(usuarios);
        this.aplicarFiltros();
        this.loading.set(false);
      },
      error: (error: any) => {
        console.error('Error al cargar usuarios:', error);
        this.loading.set(false);
      }
    });
  }

  aplicarFiltros() {
    let resultado = [...this.usuarios()];

    // Filtro por tipo
    if (this.filtroTipo() !== 'TODOS') {
      resultado = resultado.filter(u => u.tipo === this.filtroTipo());
    }

    // Filtro por búsqueda
    const busquedaLower = this.busqueda().toLowerCase();
    if (busquedaLower) {
      resultado = resultado.filter(u => 
        u.nombre.toLowerCase().includes(busquedaLower) ||
        u.email.toLowerCase().includes(busquedaLower) ||
        u.telefono.includes(busquedaLower)
      );
    }

    this.usuariosFiltrados.set(resultado);
  }

  cambiarFiltroTipo(tipo: string) {
    this.filtroTipo.set(tipo);
    this.aplicarFiltros();
  }

  buscar(event: Event) {
    const input = event.target as HTMLInputElement;
    this.busqueda.set(input.value);
    this.aplicarFiltros();
  }

  nuevoUsuario() {
    this.router.navigate(['/usuarios/crear']);
  }

  editarUsuario(id: number | undefined) {
    if (id) {
      this.router.navigate(['/usuarios/editar', id]);
    }
  }

  eliminarUsuario(id: number | undefined) {
    if (!id) return;
    
    if (!confirm('¿Estás seguro de que deseas eliminar este usuario?')) {
      return;
    }

    this.usuarioService.eliminarUsuario(id).subscribe({
      next: () => {
        this.cargarUsuarios();
      },
      error: (error: any) => {
        console.error('Error al eliminar usuario:', error);
        alert('Error al eliminar el usuario');
      }
    });
  }

  getBadgeClass(tipo: TipoUsuario): string {
    return tipo === TipoUsuario.CLIENTE ? 'badge-cliente' : 'badge-repartidor';
  }

  getEstadoBadgeClass(activo: boolean | undefined): string {
    return activo === true ? 'badge-activo' : 'badge-inactivo';
  }
}