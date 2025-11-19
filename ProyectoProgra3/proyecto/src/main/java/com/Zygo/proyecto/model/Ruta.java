package com.Zygo.proyecto.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "rutas")
public class Ruta {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Double origenLatitud;
    
    @Column(nullable = false)
    private Double origenLongitud;
    
    @Column(nullable = false)
    private Double destinoLatitud;
    
    @Column(nullable = false)
    private Double destinoLongitud;
    
    @Column(nullable = false)
    private Double distanciaKm;
    
    @Column(nullable = false)
    private Integer tiempoEstimadoMinutos;
    
    @Column(nullable = false)
    private Double costoEstimado;
    
    @Column(columnDefinition = "TEXT")
    private String instrucciones;
    
    @Column(columnDefinition = "JSON")
    private String nodos;
    
    @Column(columnDefinition = "JSON")
    private String segmentos;
    
    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    
    @Column(name = "pedido_id")
    private Long pedidoId;
    
    @Column(name = "activo")
    private Boolean activo = true;
    
    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Double getOrigenLatitud() { return origenLatitud; }
    public void setOrigenLatitud(Double origenLatitud) { this.origenLatitud = origenLatitud; }
    
    public Double getOrigenLongitud() { return origenLongitud; }
    public void setOrigenLongitud(Double origenLongitud) { this.origenLongitud = origenLongitud; }
    
    public Double getDestinoLatitud() { return destinoLatitud; }
    public void setDestinoLatitud(Double destinoLatitud) { this.destinoLatitud = destinoLatitud; }
    
    public Double getDestinoLongitud() { return destinoLongitud; }
    public void setDestinoLongitud(Double destinoLongitud) { this.destinoLongitud = destinoLongitud; }
    
    public Double getDistanciaKm() { return distanciaKm; }
    public void setDistanciaKm(Double distanciaKm) { this.distanciaKm = distanciaKm; }
    
    public Integer getTiempoEstimadoMinutos() { return tiempoEstimadoMinutos; }
    public void setTiempoEstimadoMinutos(Integer tiempoEstimadoMinutos) { this.tiempoEstimadoMinutos = tiempoEstimadoMinutos; }
    
    public Double getCostoEstimado() { return costoEstimado; }
    public void setCostoEstimado(Double costoEstimado) { this.costoEstimado = costoEstimado; }
    
    public String getInstrucciones() { return instrucciones; }
    public void setInstrucciones(String instrucciones) { this.instrucciones = instrucciones; }
    
    public String getNodos() { return nodos; }
    public void setNodos(String nodos) { this.nodos = nodos; }
    
    public String getSegmentos() { return segmentos; }
    public void setSegmentos(String segmentos) { this.segmentos = segmentos; }
    
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    
    public Long getPedidoId() { return pedidoId; }
    public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }
    
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}