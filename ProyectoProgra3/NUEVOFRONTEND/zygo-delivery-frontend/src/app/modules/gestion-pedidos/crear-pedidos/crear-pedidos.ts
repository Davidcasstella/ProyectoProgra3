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
  styleUrl: './crear-pedidos.css'
})
export class CrearPedidos implements OnInit {
  @ViewChild(SelectorUbicacionComponent) mapaComponent!: SelectorUbicacionComponent;
  
  pedidoForm!: FormGroup;
  clientes: Usuario[] = [];
  repartidores: Usuario[] = [];
  
  // ✅ Estados disponibles para el selector
  estadosDisponibles = [
    { valor: EstadoPedido.PENDIENTE, etiqueta: '⏳ Pendiente', descripcion: 'En espera de asignación' },
    { valor: EstadoPedido.ASIGNADO, etiqueta: '📋 Asignado', descripcion: 'Asignado a un repartidor' },
    { valor: EstadoPedido.EN_CAMINO, etiqueta: '🚴 En Camino', descripcion: 'El repartidor está en ruta' },
    { valor: EstadoPedido.ENTREGADO, etiqueta: '✅ Entregado', descripcion: 'Pedido completado' },
    { valor: EstadoPedido.CANCELADO, etiqueta: '❌ Cancelado', descripcion: 'Pedido cancelado' }
  ];
  
  // 🎯 WIZARD: Control de pasos (ahora 6 pasos)
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
    private lugarService: LugarService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.inicializarFormulario();
    this.cargarUsuarios();
  }

  /**
   * Inicializa el formulario con el campo de estado
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

  // ========================================
  // 🎯 FUNCIONES DEL WIZARD
  // ========================================

  /**
   * Avanza al siguiente paso si la validación del paso actual es correcta
   */
  pasoSiguiente(): void {
    if (!this.puedeContinuar()) {
      this.mostrarError('Completa los campos requeridos antes de continuar');
      return;
    }

    if (this.pasoActual < this.totalPasos) {
      this.pasoActual++;
      window.scrollTo({ top: 0, behavior: 'smooth' });

      // Si llegamos al paso 6, dibujamos la ruta en el mapa
      if (this.pasoActual === 6 && this.mapaComponent) {
        setTimeout(() => {
          this.dibujarRutaEnMapa();
        }, 300);
      }
    }
  }

  /**
   * Retrocede al paso anterior
   */
  pasoAnterior(): void {
    if (this.pasoActual > 1) {
      this.pasoActual--;
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  /**
   * Navega directamente a un paso específico (usado desde el resumen)
   */
  irAPaso(paso: number): void {
    if (paso >= 1 && paso <= this.totalPasos) {
      this.pasoActual = paso;
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  /**
   * Valida si el usuario puede continuar al siguiente paso
   */
  puedeContinuar(): boolean {
    switch (this.pasoActual) {
      case 1: // Cliente
        return this.clienteId?.valid || false;
      
      case 2: // Descripción
        return this.descripcion?.valid || false;
      
      case 3: // Asignación (siempre puede continuar, el repartidor es opcional)
        return this.estado?.valid || false;
      
      case 4: // Ubicaciones
        return !!(this.origenCoordenadas && this.destinoCoordenadas && this.distanciaCalculada);
      
      case 5: // Resumen
        return this.pedidoForm.valid && 
               !!(this.origenCoordenadas && this.destinoCoordenadas && this.distanciaCalculada);
      
      case 6: // Ruta (siempre puede continuar al botón de crear)
        return true;
      
      default:
        return false;
    }
  }

  /**
   * Selecciona un estado desde las cards
   */
  seleccionarEstado(estado: EstadoPedido): void {
    this.pedidoForm.patchValue({ estado });
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
      this.ocultarPanelSelectorUbicacion();
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
      this.ocultarPanelSelectorUbicacion();
    }
  }

  /**
   * Oculta el panel interno del componente selector-ubicacion
   */
  ocultarPanelSelectorUbicacion(): void {
    setTimeout(() => {
      // Buscar y ocultar todos los paneles posibles del componente hijo
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
   * Dibuja la ruta en el mapa del paso 6
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

    console.log('🎨 Dibujando ruta en el mapa del paso 6...');

    // Calcular de nuevo la ruta para obtener la información completa
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

  // ========================================
  // 📤 ENVÍO DEL FORMULARIO
  // ========================================

  /**
   * Enviar formulario - ahora sin redirección
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
        console.log('🆔 ID del nuevo pedido:', response.id);
        
        this.pedidoCreado = true;
        this.cargando = false;
        this.mostrarExito('¡Pedido creado exitosamente! Puedes crear otro pedido o volver al inicio.');
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
    if (this.pedidoCreado) {
      // Si ya se creó el pedido, podemos ir a la lista
      this.router.navigate(['/pedidos/listar-pedidos']);
    } else {
      // Si no, volver al dashboard
      this.router.navigate(['/dashboard']);
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