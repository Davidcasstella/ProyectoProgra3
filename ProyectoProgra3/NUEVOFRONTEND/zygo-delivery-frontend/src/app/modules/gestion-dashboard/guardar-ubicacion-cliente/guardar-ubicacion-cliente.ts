// src/app/modules/gestion-dashboard/guardar-ubicacion-cliente/guardar-ubicacion-cliente.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClienteService } from '../../../services/cliente.service';
import { SelectorUbicacionComponent } from '../../gestion-mapas/selector-ubicacion/selector-ubicacion';
import { Coordenadas } from '../../../models/ruta.model';
import { Router } from '@angular/router';

@Component({
  selector: 'app-guardar-ubicacion-cliente',
  standalone: true,
  imports: [CommonModule, SelectorUbicacionComponent],
  template: `
    <div class="ubicacion-container">
      <div class="ubicacion-header">
        <h1>📍 Registra tu Ubicación</h1>
        <p class="subtitle">Necesitamos saber dónde estás para entregar tus pedidos</p>
      </div>

      <!-- Alertas -->
      <div *ngIf="error" class="alert alert-error">
        ❌ {{ error }}
      </div>

      <div *ngIf="exito" class="alert alert-success">
        ✅ {{ exito }}
      </div>

      <!-- Card principal -->
      <div class="ubicacion-card">
        <div class="instrucciones-section">
          <div class="instruccion-item">
            <div class="instruccion-numero">1</div>
            <div class="instruccion-content">
              <strong>Haz clic en el mapa</strong>
              <p>Selecciona el punto donde estás ubicado</p>
            </div>
          </div>

          <div class="instruccion-item">
            <div class="instruccion-numero">2</div>
            <div class="instruccion-content">
              <strong>Confirma tu ubicación</strong>
              <p>Verifica que el marcador esté en el lugar correcto</p>
            </div>
          </div>

          <div class="instruccion-item">
            <div class="instruccion-numero">3</div>
            <div class="instruccion-content">
              <strong>Guarda tu ubicación</strong>
              <p>Presiona el botón "Guardar Ubicación"</p>
            </div>
          </div>
        </div>

        <!-- Estado actual -->
        <div *ngIf="ubicacionActual" class="ubicacion-actual">
          <div class="ubicacion-info">
            <span class="icono">✅</span>
            <div>
              <strong>Ubicación Actual:</strong>
              <p>{{ ubicacionActual.latitud.toFixed(4) }}, {{ ubicacionActual.longitud.toFixed(4) }}</p>
            </div>
          </div>
        </div>

        <div *ngIf="!ubicacionActual" class="ubicacion-pendiente">
          <div class="ubicacion-info">
            <span class="icono">⏳</span>
            <div>
              <strong>Sin ubicación registrada</strong>
              <p>Haz clic en el mapa para seleccionar tu ubicación</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Mapa -->
      <div class="mapa-section">
        <app-selector-ubicacion
          [modoSeleccionSimple]="true"
          (ubicacionSeleccionada)="onUbicacionSeleccionada($event)">
        </app-selector-ubicacion>
      </div>

      <!-- Botones de acción -->
      <div class="botones-accion">
        <button 
          class="btn btn-secundario"
          (click)="volverAtras()">
          ← Volver
        </button>

        <button 
          class="btn btn-primario"
          [disabled]="!ubicacionSeleccionada || guardando"
          (click)="guardarUbicacion()">
          <span *ngIf="!guardando">💾 Guardar Ubicación</span>
          <span *ngIf="guardando">⏳ Guardando...</span>
        </button>

        <button 
          class="btn btn-exito"
          *ngIf="ubicacionActual && !guardando"
          (click)="continuarAlPedido()">
          ✅ Continuar al Pedido →
        </button>
      </div>
    </div>
  `,
  styles: [`
    .ubicacion-container {
      padding: 20px;
      max-width: 900px;
      margin: 0 auto;
    }

    .ubicacion-header {
      text-align: center;
      margin-bottom: 30px;
    }

    .ubicacion-header h1 {
      font-size: 2em;
      margin: 0 0 10px 0;
      color: #2c3e50;
    }

    .subtitle {
      color: #7f8c8d;
      font-size: 1.1em;
    }

    .alert {
      padding: 15px 20px;
      border-radius: 8px;
      margin-bottom: 20px;
      font-weight: 500;
    }

    .alert-error {
      background-color: #fee;
      color: #c33;
      border-left: 4px solid #c33;
    }

    .alert-success {
      background-color: #efe;
      color: #3c3;
      border-left: 4px solid #3c3;
    }

    .ubicacion-card {
      background: white;
      border-radius: 12px;
      padding: 25px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      margin-bottom: 20px;
    }

    .instrucciones-section {
      margin-bottom: 25px;
    }

    .instruccion-item {
      display: flex;
      gap: 15px;
      margin-bottom: 15px;
      align-items: flex-start;
    }

    .instruccion-numero {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 40px;
      height: 40px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      border-radius: 50%;
      font-weight: bold;
      flex-shrink: 0;
    }

    .instruccion-content strong {
      display: block;
      margin-bottom: 5px;
      color: #2c3e50;
    }

    .instruccion-content p {
      margin: 0;
      color: #7f8c8d;
      font-size: 0.95em;
    }

    .ubicacion-actual,
    .ubicacion-pendiente {
      padding: 15px;
      border-radius: 8px;
      margin-top: 15px;
    }

    .ubicacion-actual {
      background: #e8f5e9;
      border-left: 4px solid #4caf50;
    }

    .ubicacion-pendiente {
      background: #fff3e0;
      border-left: 4px solid #ff9800;
    }

    .ubicacion-info {
      display: flex;
      gap: 15px;
      align-items: flex-start;
    }

    .icono {
      font-size: 1.5em;
    }

    .ubicacion-info strong {
      display: block;
      margin-bottom: 5px;
    }

    .ubicacion-info p {
      margin: 0;
      font-size: 0.9em;
      font-family: monospace;
      opacity: 0.8;
    }

    .mapa-section {
      background: white;
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      margin-bottom: 20px;
      height: 500px;
    }

    .botones-accion {
      display: flex;
      gap: 10px;
      justify-content: center;
      flex-wrap: wrap;
    }

    .btn {
      padding: 12px 24px;
      border: none;
      border-radius: 8px;
      font-size: 1em;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.3s ease;
    }

    .btn-primario {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
    }

    .btn-primario:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
    }

    .btn-primario:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .btn-exito {
      background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
      color: white;
    }

    .btn-exito:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(17, 153, 142, 0.4);
    }

    .btn-secundario {
      background: #ecf0f1;
      color: #2c3e50;
    }

    .btn-secundario:hover {
      background: #d5dbdb;
    }

    @media (max-width: 600px) {
      .ubicacion-container {
        padding: 15px;
      }

      .ubicacion-header h1 {
        font-size: 1.5em;
      }

      .mapa-section {
        height: 350px;
      }

      .botones-accion {
        flex-direction: column;
      }

      .btn {
        width: 100%;
      }
    }
  `]
})
export class GuardarUbicacionClienteComponent implements OnInit {
  
  ubicacionSeleccionada: Coordenadas | null = null;
  ubicacionActual: any = null;
  guardando = false;
  error: string | null = null;
  exito: string | null = null;

  constructor(
    private clienteService: ClienteService,
    private router: Router
  ) {}

  ngOnInit(): void {
    console.log('📍 Iniciando componente guardar ubicación del cliente');
    this.cargarUbicacionActual();
  }

  /**
   * Cargar ubicación actual si existe
   */
  cargarUbicacionActual(): void {
    this.clienteService.obtenerMiUbicacion().subscribe({
      next: (response) => {
        this.ubicacionActual = {
          latitud: response.latitud,
          longitud: response.longitud
        };
        console.log('📍 Ubicación actual cargada:', this.ubicacionActual);
      },
      error: () => {
        console.log('📍 Sin ubicación registrada aún');
      }
    });
  }

  /**
   * Evento cuando el usuario selecciona una ubicación en el mapa
   */
  onUbicacionSeleccionada(coords: Coordenadas): void {
    console.log('📍 Ubicación seleccionada:', coords);
    this.ubicacionSeleccionada = coords;
  }

  /**
   * Guardar la ubicación en el servidor
   */
  guardarUbicacion(): void {
    if (!this.ubicacionSeleccionada) {
      this.mostrarError('Por favor selecciona una ubicación en el mapa');
      return;
    }

    this.guardando = true;
    this.error = null;

    this.clienteService.guardarMiUbicacion(
      this.ubicacionSeleccionada.lat,
      this.ubicacionSeleccionada.lng
    ).subscribe({
      next: (response) => {
        this.guardando = false;
        this.ubicacionActual = {
          latitud: response.latitud,
          longitud: response.longitud
        };
        
        // Actualizar en el servicio también
        this.clienteService.actualizarUbicacionLocal({
          latitud: response.latitud,
          longitud: response.longitud
        });

        this.mostrarExito('✅ ¡Ubicación guardada exitosamente!');
        console.log('✅ Ubicación guardada:', response);
      },
      error: (err) => {
        this.guardando = false;
        console.error('❌ Error:', err);
        this.mostrarError(err.error?.error || 'Error al guardar ubicación');
      }
    });
  }

  /**
   * Continuar al formulario de crear pedido
   */
  continuarAlPedido(): void {
    console.log('➡️ Continuando al pedido...');
    this.router.navigate(['/pedidos/crear-pedidos']);
  }

  /**
   * Volver atrás
   */
  volverAtras(): void {
    this.router.navigate(['/dashboard']);
  }

  /**
   * Mostrar mensaje de error
   */
  mostrarError(mensaje: string): void {
    this.error = mensaje;
    window.scrollTo({ top: 0, behavior: 'smooth' });
    setTimeout(() => this.error = null, 5000);
  }

  /**
   * Mostrar mensaje de éxito
   */
  mostrarExito(mensaje: string): void {
    this.exito = mensaje;
    window.scrollTo({ top: 0, behavior: 'smooth' });
    setTimeout(() => this.exito = null, 5000);
  }
  /**
 * Navega a la página de crear pedido
 */
continuarAPedido(): void {
  console.log('➡️ Continuando al pedido...');
  this.router.navigate(['/pedidos/crear-pedidos']);
}
}