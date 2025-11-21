package com.Zygo.proyecto.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 📊 MODELO: Historial completo de rutas calculadas
 * 
 * Guarda TODA la información del cálculo de ruta para poder:
 * - Recrear visualmente la ruta en el mapa
 * - Ver qué nodos se visitaron
 * - Analizar tiempos y costos
 * - Auditar decisiones del sistema
 */
@Entity
@Table(name = "historial_rutas")
public class HistorialRuta {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // === RELACIONES ===
    
    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;
    
    @ManyToOne
    @JoinColumn(name = "restaurante_id")
    private Graph restaurante;
    
    @ManyToOne
    @JoinColumn(name = "nodo_cliente_id")
    private Graph nodoCliente;
    
    @ManyToOne
    @JoinColumn(name = "nodo_repartidor_id")
    private Graph nodoRepartidor;
    
    @ManyToOne
    @JoinColumn(name = "repartidor_id")
    private Usuario repartidor;
    
    // === INFORMACIÓN DEL CÁLCULO ===
    
    @Column(name = "tipo_calculo", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoCalculo tipoCalculo;
    
    @Column(name = "fecha_calculo", nullable = false)
    private LocalDateTime fechaCalculo;
    
    @Column(name = "tiempo_calculo_ms")
    private Long tiempoCalculoMs;
    
    // === DATOS DE LA RUTA ===
    
    @Column(name = "distancia_total_km")
    private Double distanciaTotalKm;
    
    @Column(name = "tiempo_estimado_min")
    private Integer tiempoEstimadoMin;
    
    @Column(name = "costo_calculado")
    private Double costoCalculado;
    
    @Column(name = "considero_trafico")
    private Boolean consideroTrafico;
    
    // === RUTA COMPLETA (JSON) ===
    // Guardamos toda la ruta como JSON para poder recrearla
    
    @Column(name = "nodos_ruta", columnDefinition = "TEXT")
    private String nodosRuta; // JSON con todos los nodos visitados
    
    @Column(name = "segmentos_ruta", columnDefinition = "TEXT")
    private String segmentosRuta; // JSON con todos los segmentos
    
    @Column(name = "instrucciones", columnDefinition = "TEXT")
    private String instrucciones; // Instrucciones paso a paso
    
    // === METADATA ===
    
    @Column(name = "visible_para")
    @Enumerated(EnumType.STRING)
    private VisibilidadHistorial visibilidad;
    
    @Column(name = "activo")
    private Boolean activo = true;
    
    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
    
    // === ENUMS ===
    
    public enum TipoCalculo {
        ASIGNACION_AUTOMATICA,      // Sistema asignó automáticamente
        RUTA_PICKUP,                // Ruta: Repartidor → Restaurante
        RUTA_DELIVERY,              // Ruta: Restaurante → Cliente
        ASIGNACION_MANUAL_ADMIN,    // Admin asignó manualmente
        RECALCULO_RUTA,             // Se recalculó la ruta
        SIMULACION                  // Solo simulación, no ejecutado
    }
    
    public enum VisibilidadHistorial {
        ADMIN_SOLO,                 // Solo admin puede ver
        ADMIN_Y_REPARTIDOR,         // Admin y repartidor asignado
        TODOS                       // Todos los autenticados
    }
    
    // === CONSTRUCTORES ===
    
    public HistorialRuta() {
        this.fechaCalculo = LocalDateTime.now();
        this.activo = true;
    }
    
    @PrePersist
    protected void onCreate() {
        if (fechaCalculo == null) {
            fechaCalculo = LocalDateTime.now();
        }
        if (activo == null) {
            activo = true;
        }
        if (visibilidad == null) {
            visibilidad = VisibilidadHistorial.ADMIN_Y_REPARTIDOR;
        }
    }
    
    // === GETTERS Y SETTERS ===
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Pedido getPedido() {
        return pedido;
    }
    
    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }
    
    public Graph getRestaurante() {
        return restaurante;
    }
    
    public void setRestaurante(Graph restaurante) {
        this.restaurante = restaurante;
    }
    
    public Graph getNodoCliente() {
        return nodoCliente;
    }
    
    public void setNodoCliente(Graph nodoCliente) {
        this.nodoCliente = nodoCliente;
    }
    
    public Graph getNodoRepartidor() {
        return nodoRepartidor;
    }
    
    public void setNodoRepartidor(Graph nodoRepartidor) {
        this.nodoRepartidor = nodoRepartidor;
    }
    
    public Usuario getRepartidor() {
        return repartidor;
    }
    
    public void setRepartidor(Usuario repartidor) {
        this.repartidor = repartidor;
    }
    
    public TipoCalculo getTipoCalculo() {
        return tipoCalculo;
    }
    
    public void setTipoCalculo(TipoCalculo tipoCalculo) {
        this.tipoCalculo = tipoCalculo;
    }
    
    public LocalDateTime getFechaCalculo() {
        return fechaCalculo;
    }
    
    public void setFechaCalculo(LocalDateTime fechaCalculo) {
        this.fechaCalculo = fechaCalculo;
    }
    
    public Long getTiempoCalculoMs() {
        return tiempoCalculoMs;
    }
    
    public void setTiempoCalculoMs(Long tiempoCalculoMs) {
        this.tiempoCalculoMs = tiempoCalculoMs;
    }
    
    public Double getDistanciaTotalKm() {
        return distanciaTotalKm;
    }
    
    public void setDistanciaTotalKm(Double distanciaTotalKm) {
        this.distanciaTotalKm = distanciaTotalKm;
    }
    
    public Integer getTiempoEstimadoMin() {
        return tiempoEstimadoMin;
    }
    
    public void setTiempoEstimadoMin(Integer tiempoEstimadoMin) {
        this.tiempoEstimadoMin = tiempoEstimadoMin;
    }
    
    public Double getCostoCalculado() {
        return costoCalculado;
    }
    
    public void setCostoCalculado(Double costoCalculado) {
        this.costoCalculado = costoCalculado;
    }
    
    public Boolean getConsideroTrafico() {
        return consideroTrafico;
    }
    
    public void setConsideroTrafico(Boolean consideroTrafico) {
        this.consideroTrafico = consideroTrafico;
    }
    
    public String getNodosRuta() {
        return nodosRuta;
    }
    
    public void setNodosRuta(String nodosRuta) {
        this.nodosRuta = nodosRuta;
    }
    
    public String getSegmentosRuta() {
        return segmentosRuta;
    }
    
    public void setSegmentosRuta(String segmentosRuta) {
        this.segmentosRuta = segmentosRuta;
    }
    
    public String getInstrucciones() {
        return instrucciones;
    }
    
    public void setInstrucciones(String instrucciones) {
        this.instrucciones = instrucciones;
    }
    
    public VisibilidadHistorial getVisibilidad() {
        return visibilidad;
    }
    
    public void setVisibilidad(VisibilidadHistorial visibilidad) {
        this.visibilidad = visibilidad;
    }
    
    public Boolean getActivo() {
        return activo;
    }
    
    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
    
    public String getObservaciones() {
        return observaciones;
    }
    
    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}