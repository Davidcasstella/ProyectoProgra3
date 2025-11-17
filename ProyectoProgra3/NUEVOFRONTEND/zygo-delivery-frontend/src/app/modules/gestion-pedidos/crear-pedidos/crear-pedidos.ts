// src/app/modules/gestion-pedidos/crear-pedidos/crear-pedidos.ts

import { Component, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PedidoService } from '../../../services/pedido.service';
import { UsuarioService } from '../../../services/usuario.service';
import { LugarService } from '../../../services/lugar.service';
import { Usuario } from '../../../models/usuario.model';
import { EstadoPedido } from '../../../models/pedido.model';
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
  
  // ✅ NUEVO: Estados disponibles para el selector
  estadosDisponibles = [
    { valor: EstadoPedido.PENDIENTE, etiqueta: '⏳ Pendiente', descripcion: 'En espera de asignación' },
    { valor: EstadoPedido.ASIGNADO, etiqueta: '📋 Asignado', descripcion: 'Asignado a un repartidor' },
    { valor: EstadoPedido.EN_CAMINO, etiqueta: '🚴 En Camino', descripcion: 'El repartidor está en ruta' },
    { valor: EstadoPedido.ENTREGADO, etiqueta: '✅ Entregado', descripcion: 'Pedido completado' },
    { valor: EstadoPedido.CANCELADO, etiqueta: '❌ Cancelado', descripcion: 'Pedido cancelado' }
  ];
  
  // Coordenadas seleccionadas desde el mapa
  origenCoordenadas: Coordenadas | null = null;
  destinoCoordenadas: Coordenadas | null = null;
  
  // Datos de ruta calculada
  distanciaCalculada: number | null = null;
  costoCalculado: number | null = null;
  tiempoEstimado: number | null = null;
  instruccionesRuta: Array<{texto: string, distancia?: string, tiempo?: string}> | null = null;
  
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
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.inicializarFormulario();
    this.cargarUsuarios();
  }

  /**
   * ✅ ACTUALIZADO: Inicializa el formulario con el campo de estado
   */
  inicializarFormulario(): void {
    this.pedidoForm = this.fb.group({
      clienteId: ['', Validators.required],
      repartidorId: [''],
      descripcion: ['', [Validators.required, Validators.minLength(10)]],
      // ✅ NUEVO: Campo de estado con PENDIENTE como valor por defecto
      estado: [EstadoPedido.PENDIENTE, Validators.required],
      direccionOrigen: [{ value: '', disabled: true }],
      direccionDestino: [{ value: '', disabled: true }],
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
   * Maneja la selección de origen desde el componente de mapa
   */
  onOrigenSeleccionado(coords: Coordenadas): void {
    console.log('🟢 Origen seleccionado:', coords);
    this.origenCoordenadas = { ...coords };
    
    this.pedidoForm.patchValue({
      direccionOrigen: `Lat: ${coords.lat.toFixed(6)}, Lng: ${coords.lng.toFixed(6)}`
    });

    if (this.destinoCoordenadas) {
      this.calcularRutaAutomatica();
    }
  }

  /**
   * Maneja la selección de destino desde el componente de mapa
   */
  onDestinoSeleccionado(coords: Coordenadas): void {
    console.log('🔴 Destino seleccionado:', coords);
    this.destinoCoordenadas = { ...coords };
    
    this.pedidoForm.patchValue({
      direccionDestino: `Lat: ${coords.lat.toFixed(6)}, Lng: ${coords.lng.toFixed(6)}`
    });

    if (this.origenCoordenadas) {
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
          
          const ruta = response.ruta || response;
          
          this.distanciaCalculada = ruta.distanciaTotal || ruta.distanciaTotalKm || 0;
          this.tiempoEstimado = ruta.tiempoEstimadoMinutos || 0;
          this.costoCalculado = ruta.costoEstimado || ((this.distanciaCalculada || 0) * 2000);

          this.procesarInstruccionesRuta(ruta);

          setTimeout(() => {
            this.pedidoForm.patchValue({
              distanciaKm: (this.distanciaCalculada || 0).toFixed(2),
              costo: (this.costoCalculado || 0).toFixed(2)
            });

            this.calculandoRuta = false;
            this.mostrarExito(`Ruta calculada: ${(this.distanciaCalculada || 0).toFixed(2)} km`);
            
            if (this.mapaComponent && ruta) {
              this.mapaComponent.dibujarRuta(ruta);
            }

            this.cdr.detectChanges();
          }, 0);
        },
        error: (err: any) => {
          console.error('❌ Error al calcular ruta:', err);
          this.calculandoRuta = false;
          this.mostrarError('Error al calcular la ruta');
        }
      });
  }

  /**
   * Procesa las instrucciones de ruta del backend
   */
  procesarInstruccionesRuta(ruta: any): void {
    this.instruccionesRuta = [];

    if (ruta.instrucciones && ruta.instrucciones.length > 0) {
      console.log('✅ Usando instrucciones del backend:', ruta.instrucciones.length);
      
      ruta.instrucciones.forEach((instruccion: string, index: number) => {
        let icono = '➡️';
        if (index === 0) icono = '🚀';
        else if (index === ruta.instrucciones.length - 1) icono = '🎯';
        else if (instruccion.toLowerCase().includes('izquierda')) icono = '⬅️';
        else if (instruccion.toLowerCase().includes('derecha')) icono = '➡️';
        else if (instruccion.toLowerCase().includes('continúa') || instruccion.toLowerCase().includes('sigue')) icono = '⬆️';
        
        this.instruccionesRuta!.push({
          texto: `${icono} ${instruccion}`
        });
      });
      
      console.log('📋 Instrucciones procesadas:', this.instruccionesRuta.length);
      return;
    }

    if (ruta.segmentos && ruta.segmentos.length > 0) {
      console.log('✅ Usando segmentos de ruta:', ruta.segmentos.length);
      
      if (ruta.nodos && ruta.nodos.length > 0) {
        this.instruccionesRuta.push({
          texto: `🚀 Inicia en: ${ruta.nodos[0].nombre || 'Punto de origen'}`
        });
      }

      ruta.segmentos.forEach((segmento: any) => {
        const distanciaTexto = segmento.distanciaKm 
          ? `${segmento.distanciaKm.toFixed(2)} km` 
          : undefined;
        const tiempoTexto = segmento.tiempoEstimadoMinutos 
          ? `${segmento.tiempoEstimadoMinutos} min` 
          : undefined;

        this.instruccionesRuta!.push({
          texto: `➡️ Continúa por ${segmento.nombreCalle || 'la calle'}`,
          distancia: distanciaTexto,
          tiempo: tiempoTexto
        });
      });

      if (ruta.nodos && ruta.nodos.length > 0) {
        this.instruccionesRuta.push({
          texto: `🎯 Llegarás a: ${ruta.nodos[ruta.nodos.length - 1].nombre || 'Punto de destino'}`
        });
      }
      
      console.log('📋 Instrucciones procesadas:', this.instruccionesRuta.length);
      return;
    }

    if (ruta.nodos && ruta.nodos.length > 0) {
      console.log('⚠️ Generando instrucciones básicas desde nodos:', ruta.nodos.length);
      
      this.instruccionesRuta.push({
        texto: `🚀 Inicia en: ${ruta.nodos[0].nombre || 'Punto de origen'}`
      });

      for (let i = 1; i < ruta.nodos.length - 1; i++) {
        this.instruccionesRuta.push({
          texto: `➡️ Pasa por: ${ruta.nodos[i].nombre || `Punto ${i}`}`
        });
      }

      this.instruccionesRuta.push({
        texto: `🎯 Llegarás a: ${ruta.nodos[ruta.nodos.length - 1].nombre || 'Punto de destino'}`
      });
      
      console.log('📋 Instrucciones procesadas:', this.instruccionesRuta.length);
      return;
    }

    console.warn('❌ No se pudieron generar instrucciones de navegación');
    this.instruccionesRuta = null;
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
    this.instruccionesRuta = null;
    
    this.pedidoForm.patchValue({
      direccionOrigen: '',
      direccionDestino: '',
      distanciaKm: '',
      costo: ''
    });

    if (this.mapaComponent) {
      this.mapaComponent.limpiarMapa();
    }
  }

  /**
   * ✅ ACTUALIZADO: Enviar formulario con el estado seleccionado
   */
  onSubmit(): void {
    console.log('📤 Intentando enviar formulario...');

    if (this.pedidoForm.invalid) {
      this.mostrarError('Por favor completa todos los campos requeridos');
      Object.keys(this.pedidoForm.controls).forEach(key => {
        const control = this.pedidoForm.get(key);
        if (control?.invalid) {
          control.markAsTouched();
        }
      });
      return;
    }

    if (!this.origenCoordenadas || !this.destinoCoordenadas) {
      this.mostrarError('Por favor selecciona origen y destino en el mapa');
      return;
    }

    if (!this.distanciaCalculada || !this.costoCalculado) {
      this.mostrarError('Por favor espera a que se calcule la ruta');
      return;
    }

    this.cargando = true;
    this.error = null;

    // ✅ ACTUALIZADO: Incluir el estado seleccionado
    const pedidoData = {
      clienteId: this.pedidoForm.get('clienteId')?.value,
      repartidorId: this.pedidoForm.get('repartidorId')?.value || null,
      descripcion: this.pedidoForm.get('descripcion')?.value,
      estado: this.pedidoForm.get('estado')?.value, // ✅ NUEVO
      direccionOrigen: this.pedidoForm.get('direccionOrigen')?.value,
      direccionDestino: this.pedidoForm.get('direccionDestino')?.value,
      distanciaKm: this.distanciaCalculada,
      costo: this.costoCalculado,
      latOrigen: this.origenCoordenadas.lat,
      lonOrigen: this.origenCoordenadas.lng,
      latDestino: this.destinoCoordenadas.lat,
      lonDestino: this.destinoCoordenadas.lng
    };

    console.log('📦 Enviando pedido:', pedidoData);

    this.pedidoService.crear(pedidoData).subscribe({
      next: (response: any) => {
        console.log('✅ Pedido creado exitosamente:', response);
        console.log('🆔 ID del nuevo pedido:', response.id);
        
        this.mostrarExito('¡Pedido creado exitosamente!');
        
        setTimeout(() => {
          console.log('🔄 Redirigiendo a lista de pedidos...');
          window.location.replace('/pedidos/listar-pedidos');
        }, 2000);
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
   * Volver atrás (al dashboard)
   */
  volverAtras(): void {
    this.router.navigate(['/dashboard']);
  }

  /**
   * Ir al Dashboard
   */
  irADashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  /**
   * Ir a Lista de Pedidos
   */
  irAListaPedidos(): void {
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
  get estado() { return this.pedidoForm.get('estado'); } // ✅ NUEVO
  get direccionOrigen() { return this.pedidoForm.get('direccionOrigen'); }
  get direccionDestino() { return this.pedidoForm.get('direccionDestino'); }
}