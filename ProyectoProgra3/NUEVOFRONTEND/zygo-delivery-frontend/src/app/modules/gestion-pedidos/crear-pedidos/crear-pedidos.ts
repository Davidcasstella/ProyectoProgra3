// src/app/modules/gestion-pedidos/crear-pedidos/crear-pedidos.ts

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
  ubicacionCliente: { latitud: number; longitud: number } | null = null;
  
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
  
  // Coordenadas seleccionadas desde el mapa
  origenCoordenadas: Coordenadas | null = null;
  destinoCoordenadas: Coordenadas | null = null;
  
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
      this.cargarUbicacionCliente();
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
    
    // Para clientes: Descripción → Ubicación (Destino) → Resumen
    this.pasos = [
      { numero: 1, titulo: 'Descripción', icono: '📝' },
      { numero: 2, titulo: 'Destino', icono: '🗺️' },
      { numero: 3, titulo: 'Resumen', icono: '📊' }
    ];
    this.totalPasos = 3;
    
    console.log('✅ Wizard ajustado:', { totalPasos: this.totalPasos, pasos: this.pasos });
  }

  /**
   * Cargar ubicación guardada del cliente
   */
  cargarUbicacionCliente(): void {
    console.log('📍 Cargando ubicación del cliente...');
    
    this.clienteService.obtenerPerfil().subscribe({
      next: (perfil: any) => {
        console.log('✅ Perfil del cliente cargado:', perfil);
        
        if (perfil.latitud && perfil.longitud) {
          this.ubicacionCliente = {
            latitud: perfil.latitud,
            longitud: perfil.longitud
          };
          
          // Pre-llenar el origen con la ubicación guardada
          this.origenCoordenadas = {
            lat: perfil.latitud,
            lng: perfil.longitud
          };
          
          this.pedidoForm.patchValue({
            clienteId: perfil.id,
            direccionOrigen: `📍 Mi ubicación: ${perfil.latitud.toFixed(6)}, ${perfil.longitud.toFixed(6)}`
          });
          
          console.log('✅ Origen pre-llenado:', this.origenCoordenadas);
          
          setTimeout(() => {
            this.mostrarExito('Tu ubicación ha sido cargada automáticamente');
            this.cdr.detectChanges();
          }, 0);
        } else {
          console.warn('⚠️ Cliente sin ubicación guardada');
          
          setTimeout(() => {
            this.mostrarError('Por favor, guarda tu ubicación primero en "Guardar Ubicación"');
            this.cdr.detectChanges();
          }, 0);
          
          setTimeout(() => {
            this.router.navigate(['/dashboard/guardar-ubicacion']);
          }, 3000);
        }
      },
      error: (err: any) => {
        console.error('❌ Error al cargar perfil del cliente:', err);
        
        setTimeout(() => {
          this.mostrarError('Error al cargar tu ubicación');
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
    console.log('⭐️ Intentando avanzar desde paso:', this.pasoActual);
    
    if (!this.puedeContinuar()) {
      if (this.esCliente && this.pasoActual === 1) {
        this.mostrarError('⚠️ La descripción debe tener al menos 10 caracteres');
        this.pedidoForm.get('descripcion')?.markAsTouched();
      } else if (this.pasoActual === 2 && this.esCliente && !this.destinoCoordenadas) {
        this.mostrarError('⚠️ Debes seleccionar el destino en el mapa');
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
   * Verifica si se puede continuar al siguiente paso
   */
  puedeContinuar(): boolean {
    if (this.esCliente) {
      // Para CLIENTES: 3 pasos
      switch (this.pasoActual) {
        case 1: // Descripción
          const descripcionValor = this.pedidoForm.get('descripcion')?.value || '';
          return descripcionValor.trim().length >= 10;
        
        case 2: // Destino
          return !!(this.origenCoordenadas && 
                   this.destinoCoordenadas && 
                   this.distanciaCalculada);
        
        case 3: // Resumen
          return this.pedidoForm.valid && 
                 !!(this.origenCoordenadas && 
                    this.destinoCoordenadas && 
                    this.distanciaCalculada);
        
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

  // ========================================
  // 🗺️ FUNCIONES DEL MAPA
  // ========================================

  /**
   * Maneja la selección de origen (solo ADMIN)
   */
  onOrigenSeleccionado(coords: Coordenadas): void {
    if (this.esCliente) {
      console.log('⚠️ Los clientes no pueden cambiar su ubicación de origen');
      return;
    }
    
    console.log('🟢 Origen seleccionado:', coords);
    this.origenCoordenadas = { ...coords };
    
    this.pedidoForm.patchValue({
      direccionOrigen: `Lat: ${coords.lat.toFixed(6)}, Lng: ${coords.lng.toFixed(6)}`
    });

    if (this.destinoCoordenadas) {
      this.calcularRutaAutomatica();
      this.ocultarPanelSelectorUbicacion();
    }
  }

  /**
   * Maneja la selección de destino
   */
  onDestinoSeleccionado(coords: Coordenadas): void {
    console.log('🔴 Destino seleccionado:', coords);
    this.destinoCoordenadas = { ...coords };
    
    this.pedidoForm.patchValue({
      direccionDestino: `Lat: ${coords.lat.toFixed(6)}, Lng: ${coords.lng.toFixed(6)}`
    });

    if (this.origenCoordenadas) {
      this.calcularRutaAutomatica();
      this.ocultarPanelSelectorUbicacion();
    }
  }

  /**
   * Oculta el panel interno del componente selector-ubicacion
   */
  ocultarPanelSelectorUbicacion(): void {
    setTimeout(() => {
      const paneles = document.querySelectorAll(
        'app-selector-ubicacion .map-sidebar, ' +
        'app-selector-ubicacion .panel-lateral, ' +
        'app-selector-ubicacion .instrucciones-panel, ' +
        'app-selector-ubicacion .ubicaciones-panel, ' +
        'app-selector-ubicacion .control-panel, ' +
        'app-selector-ubicacion [class*="panel"], ' +
        'app-selector-ubicacion [class*="sidebar"]'
      );
      
      paneles.forEach((panel: Element) => {
        const htmlPanel = panel as HTMLElement;
        htmlPanel.style.display = 'none';
        htmlPanel.style.visibility = 'hidden';
        htmlPanel.style.opacity = '0';
      });
      
      console.log('🚫 Paneles ocultos:', paneles.length);
    }, 100);
  }

  /**
   * Calcula la ruta automáticamente
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
        this.instruccionesRuta.map(i => i.texto).join(' | ') : 
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
    
    // Si es CLIENTE, no limpiar el origen
    if (!this.esCliente) {
      this.origenCoordenadas = null;
      this.pedidoForm.patchValue({
        direccionOrigen: ''
      });
    }
    
    this.destinoCoordenadas = null;
    this.distanciaCalculada = null;
    this.costoCalculado = null;
    this.tiempoEstimado = null;
    this.instruccionesRuta = null;
    
    this.pedidoForm.patchValue({
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
   * Enviar formulario
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

    const pedidoData = {
      clienteId: this.pedidoForm.get('clienteId')?.value,
      repartidorId: this.pedidoForm.get('repartidorId')?.value || null,
      descripcion: this.pedidoForm.get('descripcion')?.value,
      estado: this.pedidoForm.get('estado')?.value,
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
        
        this.pedidoCreado = true;
        this.cargando = false;
        this.mostrarExito('¡Pedido creado exitosamente! Redirigiendo...');
        
        // ✅ Redirigir después de 2 segundos
        setTimeout(() => {
          if (this.esCliente) {
            // Los clientes van a su dashboard
            this.router.navigate(['/dashboard']);
          } else {
            // Los admins van a la lista de pedidos
            this.router.navigate(['/pedidos/listar-pedidos']);
          }
        }, 2000);
      },
      error: (err: any) => {
        console.error('❌ Error al crear pedido:', err);
        this.mostrarError(err.error?.mensaje || 'Error al crear el pedido');
        this.cargando = false;
      }
    });
  }

  // ========================================
  // 🎨 UTILIDADES
  // ========================================

  /**
   * Volver atrás (al dashboard o lista de pedidos)
   */
  volverAtras(): void {
    if (this.esCliente) {
      // Los clientes siempre vuelven a su dashboard
      this.router.navigate(['/dashboard']);
    } else {
      // Los admins vuelven a la lista de pedidos si ya crearon uno, sino al dashboard
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