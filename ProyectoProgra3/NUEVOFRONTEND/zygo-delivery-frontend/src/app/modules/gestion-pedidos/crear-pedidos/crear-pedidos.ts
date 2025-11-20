// src/app/modules/gestion-pedidos/crear-pedidos/crear-pedidos.ts
// PARTE 1 DE 2

import { Component, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PedidoService } from '../../../services/pedido.service';
import { UsuarioService } from '../../../services/usuario.service';
import { ClienteService } from '../../../services/cliente.service';
import { LugarService } from '../../../services/lugar.service';
import { AuthService } from '../../../services/auth.service';
import { Usuario } from '../../../models/usuario.model';
import { EstadoPedido } from '../../../models/pedido.model';
import { Coordenadas } from '../../../models/ruta.model';
import { SelectorUbicacionComponent } from '../../gestion-mapas/selector-ubicacion/selector-ubicacion';
import { RutaService } from '../../../services/ruta.service';

@Component({
  selector: 'app-crear-pedidos',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SelectorUbicacionComponent],
  templateUrl: './crear-pedidos.html',
  styleUrl: './crear-pedidos.css'
})
export class CrearPedidos implements OnInit {
  @ViewChild(SelectorUbicacionComponent) mapaComponent!: SelectorUbicacionComponent;
  
  // Control del mapa (inicialmente desactivado)
  mostrarMapa = false;
  
  pedidoForm!: FormGroup;
  clientes: Usuario[] = [];
  repartidores: Usuario[] = [];
  
  // Detectar si el usuario es CLIENTE
  esCliente = false;
  usuarioActual: any = null;
  
  // Estados disponibles para el selector
  estadosDisponibles = [
    { valor: EstadoPedido.PENDIENTE, etiqueta: '⏳ Pendiente', descripcion: 'En espera de asignación' },
    { valor: EstadoPedido.ASIGNADO, etiqueta: '📋 Asignado', descripcion: 'Asignado a un repartidor' },
    { valor: EstadoPedido.EN_CAMINO, etiqueta: '🚴 En Camino', descripcion: 'El repartidor está en ruta' },
    { valor: EstadoPedido.ENTREGADO, etiqueta: '✅ Entregado', descripcion: 'Pedido completado' },
    { valor: EstadoPedido.CANCELADO, etiqueta: '❌ Cancelado', descripcion: 'Pedido cancelado' }
  ];
  
  // WIZARD: Control de pasos (dinámico según el rol)
  pasoActual = 1;
  totalPasos = 6;
  pasos = [
    { numero: 1, titulo: 'Cliente', icono: '👤' },
    { numero: 2, titulo: 'Descripción', icono: '📝' },
    { numero: 3, titulo: 'Asignación', icono: '🚴' },
    { numero: 4, titulo: 'Ubicaciones', icono: '🗺️' },
    { numero: 5, titulo: 'Resumen', icono: '📊' },
    { numero: 6, titulo: 'Ruta', icono: '🧭' }
  ];
  
  // ✅ NUEVA PROPIEDAD: Ubicación del cliente (un solo punto)
  ubicacionClienteSeleccionada: Coordenadas | null = null;
  
  // Coordenadas seleccionadas desde el mapa (para ADMIN)
  origenCoordenadas: Coordenadas | null = null;
  destinoCoordenadas: Coordenadas | null = null;
  
  // Control de ubicación guardada
  ubicacionGuardada = false;
  guardandoUbicacion = false;
  
  // Datos de ruta calculada
  distanciaCalculada: number | null = null;
  costoCalculado: number | null = null;
  tiempoEstimado: number | null = null;
  instruccionesRuta: Array<{texto: string, distancia?: string, tiempo?: string}> | null = null;
  
  // ID de la ruta guardada
  rutaId: number | null = null;

  // Estados
  cargando = false;
  calculandoRuta = false;
  error: string | null = null;
  exito: string | null = null;
  pedidoCreado = false;

  constructor(
    private fb: FormBuilder,
    private pedidoService: PedidoService,
    private usuarioService: UsuarioService,
    private clienteService: ClienteService,
    private lugarService: LugarService,
    private rutaService: RutaService,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    console.log('🚀 Iniciando componente crear pedido');
    
    this.inicializarFormulario();
    this.verificarTipoUsuario();
  }

  /**
   * Verificar si el usuario es CLIENTE
   */
  verificarTipoUsuario(): void {
    this.usuarioActual = this.authService.getUsuarioActual();
    console.log('👤 Usuario actual:', this.usuarioActual);

    const tipoUsuario = this.usuarioActual?.tipoUsuario || this.usuarioActual?.tipo;

    if (this.usuarioActual && tipoUsuario === 'CLIENTE') {
      console.log('✅ Usuario es CLIENTE');
      this.esCliente = true;
      this.ajustarWizardParaCliente();
      this.cargarClienteId();
    } else {
      console.log('👨‍💼 Usuario es ADMIN/REPARTIDOR');
      this.esCliente = false;
      this.cargarUsuarios();
    }
  }

  /**
   * Ajustar el wizard para clientes
   */
  ajustarWizardParaCliente(): void {
    console.log('🔧 Ajustando wizard para CLIENTE...');
    
    // Para clientes: Descripción → Ubicación (UN punto) → Resumen
    this.pasos = [
      { numero: 1, titulo: 'Descripción', icono: '📝' },
      { numero: 2, titulo: 'Ubicación', icono: '📍' },
      { numero: 3, titulo: 'Resumen', icono: '📊' }
    ];
    this.totalPasos = 3;
    
    console.log('✅ Wizard ajustado:', { totalPasos: this.totalPasos, pasos: this.pasos });
  }

  /**
   * Cargar solo el clienteId
   */
  cargarClienteId(): void {
    console.log('🔍 Cargando perfil del cliente...');
    
    this.clienteService.obtenerPerfil().subscribe({
      next: (perfil: any) => {
        console.log('✅ Perfil del cliente cargado:', perfil);
        
        this.pedidoForm.patchValue({
          clienteId: perfil.id
        });
        
        console.log('✅ Cliente ID configurado:', perfil.id);
        
        setTimeout(() => {
          this.mostrarExito('¡Listo para crear tu pedido!');
          this.cdr.detectChanges();
        }, 0);
      },
      error: (err: any) => {
        console.error('❌ Error al cargar perfil del cliente:', err);
        
        setTimeout(() => {
          this.mostrarError('Error al cargar tu perfil');
          this.cdr.detectChanges();
        }, 0);
      }
    });
  }

  /**
   * Inicializa el formulario
   */
  inicializarFormulario(): void {
    this.pedidoForm = this.fb.group({
      clienteId: ['', Validators.required],
      repartidorId: [''],
      descripcion: ['', [Validators.required, Validators.minLength(10)]],
      estado: [EstadoPedido.PENDIENTE, Validators.required],
      direccionOrigen: [{ value: '', disabled: true }],
      direccionDestino: [{ value: '', disabled: true }],
      distanciaKm: [{ value: '', disabled: true }],
      costo: [{ value: '', disabled: true }]
    });
  }

  /**
   * Carga clientes y repartidores (solo para ADMIN)
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

  // ========================================
  // 🎯 FUNCIONES DEL WIZARD
  // ========================================

  /**
   * Avanza al siguiente paso
   */
  pasoSiguiente(): void {
    console.log('⭐ Intentando avanzar desde paso:', this.pasoActual);
    
    if (!this.puedeContinuar()) {
      if (this.esCliente && this.pasoActual === 1) {
        this.mostrarError('⚠️ La descripción debe tener al menos 10 caracteres');
        this.pedidoForm.get('descripcion')?.markAsTouched();
      } else if (this.pasoActual === 2 && this.esCliente) {
        if (!this.ubicacionClienteSeleccionada) {
          this.mostrarError('⚠️ Debes seleccionar tu ubicación en el mapa');
        } else if (!this.ubicacionGuardada) {
          this.mostrarError('⚠️ Debes guardar tu ubicación antes de continuar');
        }
      } else {
        this.mostrarError('⚠️ Completa los campos requeridos antes de continuar');
      }
      return;
    }

    if (this.pasoActual < this.totalPasos) {
      this.pasoActual++;
      console.log('✅ Avanzando a paso:', this.pasoActual);
      
      // ✅ Activar el mapa solo cuando llegues al paso 2 y seas CLIENTE
      if (this.esCliente && this.pasoActual === 2) {
        setTimeout(() => {
          this.mostrarMapa = true;
          console.log('🗺️ Mapa activado');
          this.cdr.detectChanges();
        }, 100);
      }
      
      // Solo dibujar ruta si NO es cliente y llegamos al paso 6
      if (!this.esCliente && this.pasoActual === 6 && this.mapaComponent) {
        setTimeout(() => {
          this.dibujarRutaEnMapa();
        }, 300);
      }
      
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  /**
   * ✅ ACTUALIZADO: Verifica si se puede continuar al siguiente paso
   */
  puedeContinuar(): boolean {
    if (this.esCliente) {
      // Para CLIENTES: 3 pasos
      switch (this.pasoActual) {
        case 1: // Descripción
          const descripcionValor = this.pedidoForm.get('descripcion')?.value || '';
          return descripcionValor.trim().length >= 10;
        
        case 2: // Ubicación del cliente (UN SOLO PUNTO + guardado)
          return !!(this.ubicacionClienteSeleccionada && this.ubicacionGuardada);
        
        case 3: // Resumen
          return this.pedidoForm.valid && 
                 !!(this.ubicacionClienteSeleccionada && this.ubicacionGuardada);
        
        default:
          return false;
      }
    } else {
      // Para ADMIN: 6 pasos originales
      switch (this.pasoActual) {
        case 1: return this.clienteId?.valid || false;
        case 2: return this.descripcion?.valid || false;
        case 3: return this.estado?.valid || false;
        case 4: return !!(this.origenCoordenadas && this.destinoCoordenadas && this.distanciaCalculada);
        case 5: return this.pedidoForm.valid && !!(this.origenCoordenadas && this.destinoCoordenadas && this.distanciaCalculada);
        case 6: return true;
        default: return false;
      }
    }
  }

  /**
   * Retrocede al paso anterior
   */
  pasoAnterior(): void {
    if (this.pasoActual > 1) {
      this.pasoActual--;
      
      // Desactivar el mapa si vuelves al paso 1 siendo cliente
      if (this.esCliente && this.pasoActual === 1) {
        this.mostrarMapa = false;
        console.log('🗺️ Mapa desactivado');
      }
      
      console.log('⬅️ Retrocediendo a paso:', this.pasoActual);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  /**
   * Navega directamente a un paso específico
   */
  irAPaso(paso: number): void {
    if (paso >= 1 && paso <= this.totalPasos) {
      this.pasoActual = paso;
      
      // Activar mapa si navegas al paso 2 siendo cliente
      if (this.esCliente && this.pasoActual === 2) {
        setTimeout(() => {
          this.mostrarMapa = true;
          this.cdr.detectChanges();
        }, 100);
      }
      
      console.log('🎯 Navegando a paso:', this.pasoActual);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  /**
   * Selecciona un estado desde las cards
   */
  seleccionarEstado(estado: EstadoPedido): void {
    this.pedidoForm.patchValue({ estado });
    console.log('🏷️ Estado seleccionado:', estado);
  }

  /**
   * Obtiene la etiqueta visual de un estado
   */
  obtenerEtiquetaEstado(valor: EstadoPedido | null): string {
    if (!valor) return '';
    const estado = this.estadosDisponibles.find(e => e.valor === valor);
    return estado ? estado.etiqueta : valor;
  }

  // ========================================
  // 📋 GETTERS PARA DATOS SELECCIONADOS
  // ========================================

  get clienteSeleccionado(): Usuario | undefined {
    const clienteId = this.pedidoForm.get('clienteId')?.value;
    return this.clientes.find(c => c.id === clienteId);
  }

  get repartidorSeleccionado(): Usuario | undefined {
    const repartidorId = this.pedidoForm.get('repartidorId')?.value;
    return repartidorId ? this.repartidores.find(r => r.id === repartidorId) : undefined;
  }

  // CONTINÚA EN PARTE 2...
  // src/app/modules/gestion-pedidos/crear-pedidos/crear-pedidos.ts
// PARTE 2 DE 2 - Continúa desde la Parte 1

 // src/app/modules/gestion-pedidos/crear-pedidos/crear-pedidos.ts
// PARTE 2 DE 2 - Continúa desde la Parte 1

  // ========================================
  // 🗺️ FUNCIONES DEL MAPA - NUEVAS PARA CLIENTE
  // ========================================

  /**
   * 📍 NUEVO: Maneja cuando el CLIENTE selecciona SU ubicación (un solo punto)
   */
  onUbicacionClienteSeleccionada(coords: Coordenadas): void {
    console.log('📍 Cliente seleccionó su ubicación:', coords);
    
    this.ubicacionClienteSeleccionada = { ...coords };
    this.ubicacionGuardada = false; // Reset del estado de guardado
    
    // Guardar también como origen (para compatibilidad con el formulario)
    this.origenCoordenadas = { ...coords };
    
    // Mostrar en el formulario
    this.pedidoForm.patchValue({
      direccionOrigen: `Lat: ${coords.lat.toFixed(6)}, Lng: ${coords.lng.toFixed(6)}`
    });
    
    this.cdr.detectChanges();
  }

  /**
   * 💾 GUARDAR la ubicación del cliente en el backend
   */
  guardarUbicacionCliente(): void {
    if (!this.ubicacionClienteSeleccionada) {
      this.mostrarError('⚠️ Debes seleccionar tu ubicación primero');
      return;
    }

    this.guardandoUbicacion = true;
    console.log('💾 Guardando ubicación del cliente...', this.ubicacionClienteSeleccionada);

    this.clienteService.guardarMiUbicacion(
      this.ubicacionClienteSeleccionada.lat,
      this.ubicacionClienteSeleccionada.lng
    ).subscribe({
      next: (response) => {
        this.guardandoUbicacion = false;
        this.ubicacionGuardada = true;
        
        console.log('✅ Ubicación guardada exitosamente:', response);
        this.mostrarExito('✅ ¡Ubicación guardada exitosamente!');
        
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.guardandoUbicacion = false;
        console.error('❌ Error al guardar ubicación:', err);
        this.mostrarError(err.error?.error || 'Error al guardar tu ubicación');
      }
    });
  }

  /**
   * 🗑️ Limpiar la ubicación seleccionada del cliente
   */
  limpiarUbicacionCliente(): void {
    console.log('🗑️ Limpiando ubicación del cliente');
    
    this.ubicacionClienteSeleccionada = null;
    this.ubicacionGuardada = false;
    this.origenCoordenadas = null;
    
    this.pedidoForm.patchValue({
      direccionOrigen: ''
    });

    if (this.mapaComponent) {
      this.mapaComponent.limpiarMapa();
    }
    
    this.cdr.detectChanges();
  }

  // ========================================
  // 🗺️ FUNCIONES DEL MAPA - PARA ADMIN
  // ========================================

  /**
   * Maneja la selección de origen (ADMIN)
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
   * Maneja la selección de destino (ADMIN o segundo punto)
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
   * Calcula la ruta automáticamente (para ADMIN)
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

            this.guardarRutaEnBD(ruta);

            this.calculandoRuta = false;
            this.mostrarExito(`Ruta calculada: ${(this.distanciaCalculada || 0).toFixed(2)} km`);
            
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
   * Guardar ruta en BD
   */
  private guardarRutaEnBD(rutaResponse: any): void {
    const rutaGuardar = {
      origenLatitud: this.origenCoordenadas?.lat,
      origenLongitud: this.origenCoordenadas?.lng,
      destinoLatitud: this.destinoCoordenadas?.lat,
      destinoLongitud: this.destinoCoordenadas?.lng,
      distanciaKm: this.distanciaCalculada,
      tiempoEstimadoMinutos: this.tiempoEstimado,
      costoEstimado: this.costoCalculado,
      instrucciones: this.instruccionesRuta ? 
        this.instruccionesRuta.map((i: any) => i.texto).join(' | ') : 
        'Ruta directa',
      nodos: rutaResponse.nodos || [],
      segmentos: rutaResponse.segmentos || []
    };

    console.log('💾 Guardando ruta:', rutaGuardar);

    this.rutaService.guardarRuta(rutaGuardar).subscribe({
      next: (response: any) => {
        console.log('✅ Ruta guardada exitosamente:', response);
        this.rutaId = response.id || response;
        console.log('🆔 ID de ruta guardado:', this.rutaId);
      },
      error: (err: any) => {
        console.error('❌ Error al guardar la ruta en BD:', err);
      }
    });
  }

  /**
   * Dibuja la ruta en el mapa del último paso
   */
  dibujarRutaEnMapa(): void {
    if (!this.mapaComponent) {
      console.warn('⚠️ Componente de mapa no disponible');
      return;
    }

    if (!this.origenCoordenadas || !this.destinoCoordenadas) {
      console.warn('⚠️ No hay coordenadas para dibujar');
      return;
    }

    console.log('🎨 Dibujando ruta en el mapa...');

    const request = {
      latOrigen: this.origenCoordenadas.lat,
      lonOrigen: this.origenCoordenadas.lng,
      latDestino: this.destinoCoordenadas.lat,
      lonDestino: this.destinoCoordenadas.lng
    };

    this.lugarService.calcularRutaPorCoordenadas(request)
      .subscribe({
        next: (response: any) => {
          const ruta = response.ruta || response;
          
          if (this.mapaComponent && ruta) {
            this.mapaComponent.dibujarRuta(ruta);
            console.log('✅ Ruta dibujada en el mapa');
          }
        },
        error: (err: any) => {
          console.error('❌ Error al dibujar ruta:', err);
        }
      });
  }

  /**
   * Procesa las instrucciones de ruta del backend
   */
  procesarInstruccionesRuta(ruta: any): void {
    this.instruccionesRuta = [];

    if (ruta.instrucciones && ruta.instrucciones.length > 0) {
      console.log('✅ Usando instrucciones del backend');
      
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
      
      return;
    }

    if (ruta.segmentos && ruta.segmentos.length > 0) {
      console.log('✅ Usando segmentos de ruta');
      
      if (ruta.nodos && ruta.nodos.length > 0) {
        this.instruccionesRuta.push({
          texto: `🚀 Inicia en: ${ruta.nodos[0].nombre || 'Punto de origen'}`
        });
      }

      ruta.segmentos.forEach((segmento: any) => {
        this.instruccionesRuta!.push({
          texto: `➡️ Continúa por ${segmento.nombreCalle || 'la calle'}`,
          distancia: segmento.distanciaKm ? `${segmento.distanciaKm.toFixed(2)} km` : undefined,
          tiempo: segmento.tiempoEstimadoMinutos ? `${segmento.tiempoEstimadoMinutos} min` : undefined
        });
      });

      if (ruta.nodos && ruta.nodos.length > 0) {
        this.instruccionesRuta.push({
          texto: `🎯 Llegarás a: ${ruta.nodos[ruta.nodos.length - 1].nombre || 'Punto de destino'}`
        });
      }
      
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

  // ========================================
  // 📤 ENVÍO DEL FORMULARIO
  // ========================================

  /**
   * ✅ ACTUALIZADO: Enviar formulario
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

    // Validación específica para CLIENTES
    if (this.esCliente) {
      if (!this.ubicacionClienteSeleccionada) {
        this.mostrarError('Por favor selecciona tu ubicación en el mapa');
        return;
      }

      if (!this.ubicacionGuardada) {
        this.mostrarError('⚠️ Debes guardar tu ubicación antes de crear el pedido');
        return;
      }
    } else {
      // Validación para ADMIN
      if (!this.origenCoordenadas || !this.destinoCoordenadas) {
        this.mostrarError('Por favor selecciona origen y destino en el mapa');
        return;
      }

      if (!this.distanciaCalculada || !this.costoCalculado) {
        this.mostrarError('Por favor espera a que se calcule la ruta');
        return;
      }
    }

    this.cargando = true;
    this.error = null;

    // 🔥 CORREGIDO: Crear objeto con datos básicos
    const pedidoData: any = {
      clienteId: this.pedidoForm.get('clienteId')?.value,
      descripcion: this.pedidoForm.get('descripcion')?.value,
      estado: this.pedidoForm.get('estado')?.value
    };

    // Para CLIENTES: enviamos su ubicación como origen y destino temporales
    if (this.esCliente) {
      // Enviamos la ubicación del cliente en el formato que espera el backend
      const ubicacionTexto = `Cliente - Lat: ${this.ubicacionClienteSeleccionada!.lat.toFixed(6)}, Lng: ${this.ubicacionClienteSeleccionada!.lng.toFixed(6)}`;
      
      pedidoData.direccionOrigen = ubicacionTexto;
      pedidoData.direccionDestino = ubicacionTexto;
      pedidoData.latOrigen = this.ubicacionClienteSeleccionada!.lat;
      pedidoData.lonOrigen = this.ubicacionClienteSeleccionada!.lng;
      pedidoData.latDestino = this.ubicacionClienteSeleccionada!.lat;
      pedidoData.lonDestino = this.ubicacionClienteSeleccionada!.lng;
      pedidoData.distanciaKm = 0.01; // 🔥 Valor mínimo positivo (el backend calculará la ruta real)
      pedidoData.costo = 1000; // 🔥 Costo mínimo positivo (el backend calculará el costo real)
      pedidoData.repartidorId = null; // Sin asignar inicialmente
      
      console.log('📦 DATOS DEL PEDIDO A ENVIAR:', JSON.stringify(pedidoData, null, 2));
      
      // El backend debe calcular después: Repartidor → Restaurante → Cliente
      console.log('📦 Enviando pedido de cliente (con su ubicación):', pedidoData);
    } else {
      // Para ADMIN: enviamos origen y destino completos
      pedidoData.repartidorId = this.pedidoForm.get('repartidorId')?.value || null;
      pedidoData.direccionOrigen = this.pedidoForm.get('direccionOrigen')?.value;
      pedidoData.direccionDestino = this.pedidoForm.get('direccionDestino')?.value;
      pedidoData.distanciaKm = this.distanciaCalculada;
      pedidoData.costo = this.costoCalculado;
      pedidoData.latOrigen = this.origenCoordenadas!.lat;
      pedidoData.lonOrigen = this.origenCoordenadas!.lng;
      pedidoData.latDestino = this.destinoCoordenadas!.lat;
      pedidoData.lonDestino = this.destinoCoordenadas!.lng;
      
      console.log('📦 Enviando pedido de admin:', pedidoData);
    }

    this.pedidoService.crear(pedidoData).subscribe({
      next: (response: any) => {
        console.log('✅ Pedido creado exitosamente:', response);
        
        this.pedidoCreado = true;
        this.cargando = false;
        this.mostrarExito('¡Pedido creado exitosamente! Redirigiendo...');
        
        // Redirigir después de 2 segundos
        setTimeout(() => {
          if (this.esCliente) {
            this.router.navigate(['/dashboard']);
          } else {
            this.router.navigate(['/pedidos/listar-pedidos']);
          }
        }, 2000);
      },
      error: (err: any) => {
        console.error('❌ Error al crear pedido:', err);
        console.error('❌ Detalles del error:', JSON.stringify(err, null, 2));
        this.mostrarError(err.error?.mensaje || err.error?.message || 'Error al crear el pedido');
        this.cargando = false;
      }
    });
  }

  // ========================================
  // 🎨 UTILIDADES
  // ========================================

  /**
   * Volver atrás
   */
  volverAtras(): void {
    if (this.esCliente) {
      this.router.navigate(['/dashboard']);
    } else {
      if (this.pedidoCreado) {
        this.router.navigate(['/pedidos/listar-pedidos']);
      } else {
        this.router.navigate(['/dashboard']);
      }
    }
  }

  /**
   * Muestra mensaje de error
   */
  mostrarError(mensaje: string): void {
    this.error = mensaje;
    window.scrollTo({ top: 0, behavior: 'smooth' });
    setTimeout(() => this.error = null, 5000);
  }

  /**
   * Muestra mensaje de éxito
   */
  mostrarExito(mensaje: string): void {
    this.exito = mensaje;
    window.scrollTo({ top: 0, behavior: 'smooth' });
    setTimeout(() => this.exito = null, 5000);
  }

  /**
   * Getters para validación
   */
  get clienteId() { return this.pedidoForm.get('clienteId'); }
  get descripcion() { return this.pedidoForm.get('descripcion'); }
  get estado() { return this.pedidoForm.get('estado'); }
  get direccionOrigen() { return this.pedidoForm.get('direccionOrigen'); }
  get direccionDestino() { return this.pedidoForm.get('direccionDestino'); }
}

// FIN DEL ARCHIVO