package com.Zygo.proyecto.model;

import jakarta.persistence.*;

/**
 * Entidad que representa una arista/conexión entre dos nodos
 * Representa una calle o ruta entre dos ubicaciones
 */
@Entity
@Table(name = "aristas_grafo")
public class Edge {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "nodo_origen_id", nullable = false)
    private Graph nodoOrigen;
    
    @ManyToOne
    @JoinColumn(name = "nodo_destino_id", nullable = false)
    private Graph nodoDestino;
    
    @Column(nullable = false)
    private Double distanciaKm;
    
    @Column(name = "tiempo_estimado_minutos", nullable = false)
    private Integer tiempoEstimadoMinutos;
    
    @Column(name = "factor_trafico")
    private Double factorTrafico = 1.0; // 1.0 = normal, >1.0 = congestionado
    
    @Column(name = "es_bidireccional")
    private Boolean esBidireccional = true;
    
    @Enumerated(EnumType.STRING)
    private TipoCalle tipoCalle;
    
    @Column(name = "nombre_calle")
    private String nombreCalle;
    
    @Column(nullable = false)
    private Boolean activo = true;
    
    // ✅ ENUM ACTUALIZADO CON ACCESO_RESTAURANTE
    public enum TipoCalle {
        AVENIDA_PRINCIPAL,
        CALLE_SECUNDARIA,
        CALLEJON,
        CICLOVIA,
        PEATONAL,
        ACCESO_RESTAURANTE  // ← NUEVO: Para conexiones de restaurantes al grafo
    }
    
    // Constructores
    public Edge() {}
    
    public Edge(Graph nodoOrigen, Graph nodoDestino, Double distanciaKm, Integer tiempoEstimadoMinutos) {
        this.nodoOrigen = nodoOrigen;
        this.nodoDestino = nodoDestino;
        this.distanciaKm = distanciaKm;
        this.tiempoEstimadoMinutos = tiempoEstimadoMinutos;
    }
    
    // Método para calcular el peso de la arista considerando tráfico
    public Double getPesoConTrafico() {
        return distanciaKm * factorTrafico;
    }
    
    // Método para calcular tiempo estimado con tráfico
    public Integer getTiempoConTrafico() {
        return (int)(tiempoEstimadoMinutos * factorTrafico);
    }
    
    // Getters y Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Graph getNodoOrigen() {
        return nodoOrigen;
    }
    
    public void setNodoOrigen(Graph nodoOrigen) {
        this.nodoOrigen = nodoOrigen;
    }
    
    public Graph getNodoDestino() {
        return nodoDestino;
    }
    
    public void setNodoDestino(Graph nodoDestino) {
        this.nodoDestino = nodoDestino;
    }
    
    public Double getDistanciaKm() {
        return distanciaKm;
    }
    
    public void setDistanciaKm(Double distanciaKm) {
        this.distanciaKm = distanciaKm;
    }
    
    public Integer getTiempoEstimadoMinutos() {
        return tiempoEstimadoMinutos;
    }
    
    public void setTiempoEstimadoMinutos(Integer tiempoEstimadoMinutos) {
        this.tiempoEstimadoMinutos = tiempoEstimadoMinutos;
    }
    
    public Double getFactorTrafico() {
        return factorTrafico;
    }
    
    public void setFactorTrafico(Double factorTrafico) {
        this.factorTrafico = factorTrafico;
    }
    
    public Boolean getEsBidireccional() {
        return esBidireccional;
    }
    
    public void setEsBidireccional(Boolean esBidireccional) {
        this.esBidireccional = esBidireccional;
    }
    
    public TipoCalle getTipoCalle() {
        return tipoCalle;
    }
    
    public void setTipoCalle(TipoCalle tipoCalle) {
        this.tipoCalle = tipoCalle;
    }
    
    public String getNombreCalle() {
        return nombreCalle;
    }
    
    public void setNombreCalle(String nombreCalle) {
        this.nombreCalle = nombreCalle;
    }
    
    public Boolean getActivo() {
        return activo;
    }
    
    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}