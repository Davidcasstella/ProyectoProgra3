// src/app/modules/gestion-pedidos/crear-pedidos/crear-pedidos.ts

import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PedidoService } from '../../../services/pedido.service';
import { UsuarioService } from '../../../services/usuario.service';
import { LugarService } from '../../../services/lugar.service';
import { Usuario } from '../../../models/usuario.model';
import { Coordenadas } from '../../../models/ruta.model';
import { SelectorUbicacionComponent } from '../../gestion-mapas/selector-ubicacion/selector-ubicacion';

@Component({
  selector: 'app-crear-pedidos',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SelectorUbicacionComponent],
  templateUrl: './crear-pedidos.html',
  styleUrl: './crear-pedidos.css',
})
export class CrearPedidos implements OnInit {
  @ViewChild(SelectorUbicacionComponent) mapaComponent!: SelectorUbicacionComponent;
  
  pedidoForm!: FormGroup;
  clientes: Usuario[] = [];
  repartidores: Usuario[] = [];
  
  // Estado del mapa y modal
  mostrarModalMapa = false;
  seleccionandoOrigen = false;
  seleccionandoDestino = false;
  
  // Coordenadas seleccionadas
  origenCoordenadas: Coordenadas | null = null;
  destinoCoordenadas: Coordenadas | null = null;
  
  // Datos de ruta calculada
  distanciaCalculada: number | null = null;
  costoCalculado: number | null = null;
  tiempoEstimado: number | null = null;
  
  // Estados
  cargando = false;
  calculandoRuta = false;
  error: string | null = null;
  exito: string | null = null;

  constructor(
    private fb: FormBuilder,
    private pedidoService: PedidoService,
    private usuarioService: UsuarioService,
    private lugarService: LugarService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.inicializarFormulario();
    this.cargarUsuarios();
  }

  /**
   * Inicializa el formulario de pedidos
   */
  inicializarFormulario(): void {
    this.pedidoForm = this.fb.group({
      clienteId: ['', Validators.required],
      repartidorId: [''],
      descripcion: ['', [Validators.required, Validators.minLength(10)]],
      direccionOrigen: ['', Validators.required],
      direccionDestino: ['', Validators.required],
      distanciaKm: [{ value: '', disabled: true }],
      costo: [{ value: '', disabled: true }]
    });
  }

  /**
   * Carga clientes y repartidores
   */
  cargarUsuarios(): void {
    this.usuarioService.obtenerTodosLosUsuarios().subscribe({
      next: (usuarios: Usuario[]) => {
        this.clientes = usuarios.filter((u: Usuario) => u.tipo === 'CLIENTE');
        this.repartidores = usuarios.filter((u: Usuario) => u.tipo === 'REPARTIDOR');
      },
      error: (err: any) => {
        console.error('Error al cargar usuarios:', err);
        this.mostrarError('Error al cargar usuarios');
      }
    });
  }

  /**
   * Abre modal del mapa para seleccionar origen
   */
  seleccionarOrigen(): void {
    console.log('🟢 Abriendo modal para seleccionar ORIGEN');
    this.seleccionandoOrigen = true;
    this.seleccionandoDestino = false;
    this.mostrarModalMapa = true;
  }

  /**
   * Abre modal del mapa para seleccionar destino
   */
  seleccionarDestino(): void {
    console.log('🔴 Abriendo modal para seleccionar DESTINO');
    this.seleccionandoOrigen = false;
    this.seleccionandoDestino = true;
    this.mostrarModalMapa = true;
  }

  /**
   * Maneja la selección de ubicación desde el mapa
   */
  onUbicacionSeleccionada(coords: Coordenadas): void {
    console.log('📍 Ubicación recibida del mapa:', coords);
    console.log('Estado actual - Origen:', this.seleccionandoOrigen, 'Destino:', this.seleccionandoDestino);

    if (this.seleccionandoOrigen) {
      this.origenCoordenadas = { ...coords };
      this.pedidoForm.patchValue({
        direccionOrigen: `Lat: ${coords.lat.toFixed(6)}, Lng: ${coords.lng.toFixed(6)}`
      });
      console.log('✅ Origen guardado:', this.origenCoordenadas);
      this.mostrarExito('Origen seleccionado correctamente');
      
      setTimeout(() => {
        this.cerrarModalMapa();
      }, 500);
    } 
    else if (this.seleccionandoDestino) {
      this.destinoCoordenadas = { ...coords };
      this.pedidoForm.patchValue({
        direccionDestino: `Lat: ${coords.lat.toFixed(6)}, Lng: ${coords.lng.toFixed(6)}`
      });
      console.log('✅ Destino guardado:', this.destinoCoordenadas);
      this.mostrarExito('Destino seleccionado correctamente');
      
      setTimeout(() => {
        this.cerrarModalMapa();
      }, 500);
    }

    if (this.origenCoordenadas && this.destinoCoordenadas) {
      console.log('🧭 Ambos puntos seleccionados, calculando ruta...');
      this.calcularRutaAutomatica();
    }
  }

  /**
   * Calcula la ruta automáticamente cuando se tienen ambos puntos
   */
  calcularRutaAutomatica(): void {
    if (!this.origenCoordenadas || !this.destinoCoordenadas) {
      console.warn('⚠️ No se pueden calcular la ruta sin ambas coordenadas');
      return;
    }

    this.calculandoRuta = true;
    console.log('🚀 Calculando ruta automáticamente...');
    console.log('Origen:', this.origenCoordenadas);
    console.log('Destino:', this.destinoCoordenadas);

    const request = {
      latOrigen: this.origenCoordenadas.lat,
      lonOrigen: this.origenCoordenadas.lng,
      latDestino: this.destinoCoordenadas.lat,
      lonDestino: this.destinoCoordenadas.lng
    };

    this.lugarService.calcularRutaPorCoordenadas(request)
      .subscribe({
        next: (response: any) => {
          console.log('✅ Ruta calculada:', response);
          
          // ✅ Proteger contra valores undefined
          const ruta = response.ruta || response;
          
          this.distanciaCalculada = ruta.distanciaTotal || ruta.distanciaTotalKm || 0;
          this.tiempoEstimado = ruta.tiempoEstimadoMinutos || 0;
          this.costoCalculado = (this.distanciaCalculada || 0) * 2000;

          this.pedidoForm.patchValue({
            distanciaKm: (this.distanciaCalculada || 0).toFixed(2),
            costo: (this.costoCalculado || 0).toFixed(2)
          });

          this.calculandoRuta = false;
          this.mostrarExito(`Ruta calculada: ${(this.distanciaCalculada || 0).toFixed(2)} km`);
        },
        error: (err: any) => {
          console.error('❌ Error al calcular ruta:', err);
          this.calculandoRuta = false;
          this.mostrarError('Error al calcular la ruta');
        }
      });
  }

  /**
   * Cierra el modal del mapa
   */
  cerrarModalMapa(): void {
    console.log('🚪 Cerrando modal del mapa');
    this.mostrarModalMapa = false;
    this.seleccionandoOrigen = false;
    this.seleccionandoDestino = false;
  }

  /**
   * Limpiar ubicaciones seleccionadas
   */
  limpiarUbicaciones(): void {
    console.log('🗑️ Limpiando ubicaciones');
    this.origenCoordenadas = null;
    this.destinoCoordenadas = null;
    this.distanciaCalculada = null;
    this.costoCalculado = null;
    this.tiempoEstimado = null;
    
    this.pedidoForm.patchValue({
      direccionOrigen: '',
      direccionDestino: '',
      distanciaKm: '',
      costo: ''
    });
  }

  /**
   * Enviar formulario
   */
  onSubmit(): void {
    console.log('📤 Intentando enviar formulario...');
    console.log('Formulario válido:', this.pedidoForm.valid);
    console.log('Origen:', this.origenCoordenadas);
    console.log('Destino:', this.destinoCoordenadas);

    if (this.pedidoForm.invalid) {
      this.mostrarError('Por favor completa todos los campos requeridos');
      return;
    }

    if (!this.origenCoordenadas || !this.destinoCoordenadas) {
      this.mostrarError('Por favor selecciona origen y destino en el mapa');
      return;
    }

    this.cargando = true;
    this.error = null;

    const pedidoData = {
      ...this.pedidoForm.getRawValue(),
      distanciaKm: this.distanciaCalculada,
      costo: this.costoCalculado,
      latOrigen: this.origenCoordenadas.lat,
      lonOrigen: this.origenCoordenadas.lng,
      latDestino: this.destinoCoordenadas.lat,
      lonDestino: this.destinoCoordenadas.lng
    };

    console.log('📦 Enviando pedido con coordenadas:', pedidoData);

    this.pedidoService.crear(pedidoData).subscribe({
      next: (response: any) => {
        console.log('✅ Pedido creado exitosamente:', response);
        this.mostrarExito('¡Pedido creado exitosamente!');
        setTimeout(() => {
          this.router.navigate(['/pedidos/listar-pedidos']);
        }, 1500);
      },
      error: (err: any) => {
        console.error('❌ Error al crear pedido:', err);
        this.mostrarError(err.error?.mensaje || 'Error al crear el pedido');
        this.cargando = false;
      }
    });
  }

  /**
   * Cancelar y volver
   */
  cancelar(): void {
    this.router.navigate(['/pedidos/listar-pedidos']);
  }

  /**
   * Muestra mensaje de error
   */
  mostrarError(mensaje: string): void {
    this.error = mensaje;
    setTimeout(() => this.error = null, 5000);
  }

  /**
   * Muestra mensaje de éxito
   */
  mostrarExito(mensaje: string): void {
    this.exito = mensaje;
    setTimeout(() => this.exito = null, 3000);
  }

  /**
   * Getters para validación
   */
  get clienteId() { return this.pedidoForm.get('clienteId'); }
  get descripcion() { return this.pedidoForm.get('descripcion'); }
  get direccionOrigen() { return this.pedidoForm.get('direccionOrigen'); }
  get direccionDestino() { return this.pedidoForm.get('direccionDestino'); }
}