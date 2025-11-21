package com.Zygo.proyecto.dto;

import com.Zygo.proyecto.model.HistorialRuta;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 📊 DTO para transferencia de historial de rutas
 */
public class HistorialRutaDTO {
    
    private Long id;
    private Long pedidoId;
    private String tipoPedido; // Descripción del pedido
    private String instruccionesJson;
    
    // Información de actores
    private InfoRestaurante restaurante;
    private InfoNodo nodoCliente;
    private InfoNodo nodoRepartidor;
    private InfoRepartidor repartidor;
    
    // Información del cálculo
    private String tipoCalculo;
    private LocalDateTime fechaCalculo;
    private Long tiempoCalculoMs;
    
    // Datos de la ruta
    private Double distanciaTotalKm;
    private Integer tiempoEstimadoMin;
    private Double costoCalculado;
    private Boolean consideroTrafico;
    private Long restauranteId;
private String restauranteNombre;
private Long repartidorId;
private String repartidorNombre;
private Long nodoClienteId;
private Long nodoRepartidorId;
private String nodosRutaJson;
private String segmentosRutaJson;
    
    // Ruta completa para recrear en el mapa
    private List<RutaOptimaDTO.NodoDTO> nodos;
    private List<RutaOptimaDTO.SegmentoRuta> segmentos;
    private List<String> instrucciones;
    
    // Metadata
    private String visibilidad;
    private Boolean activo;
    private String observaciones;
    
    // === RECORDS INTERNOS ===
    
    public record InfoRestaurante(
        Long id,
        String nombre,
        Double latitud,
        Double longitud
    ) {}
    
    public record InfoNodo(
        Long id,
        String nombre,
        Double latitud,
        Double longitud,
        String tipo
    ) {}
    
    public record InfoRepartidor(
        Long id,
        String nombre,
        String telefono
    ) {}
    
    // === CONSTRUCTORES ===
    
    public HistorialRutaDTO() {}
    
    // === GETTERS Y SETTERS ===
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getPedidoId() {
        return pedidoId;
    }
    
    public void setPedidoId(Long pedidoId) {
        this.pedidoId = pedidoId;
    }
    
    public String getTipoPedido() {
        return tipoPedido;
    }
    
    public void setTipoPedido(String tipoPedido) {
        this.tipoPedido = tipoPedido;
    }
    
    public InfoRestaurante getRestaurante() {
        return restaurante;
    }
    
    public void setRestaurante(InfoRestaurante restaurante) {
        this.restaurante = restaurante;
    }
    
    public InfoNodo getNodoCliente() {
        return nodoCliente;
    }
    
    public void setNodoCliente(InfoNodo nodoCliente) {
        this.nodoCliente = nodoCliente;
    }
    
    public InfoNodo getNodoRepartidor() {
        return nodoRepartidor;
    }
    
    public void setNodoRepartidor(InfoNodo nodoRepartidor) {
        this.nodoRepartidor = nodoRepartidor;
    }
    
    public InfoRepartidor getRepartidor() {
        return repartidor;
    }
    
    public void setRepartidor(InfoRepartidor repartidor) {
        this.repartidor = repartidor;
    }
    
    public String getTipoCalculo() {
        return tipoCalculo;
    }
    
    public void setTipoCalculo(String tipoCalculo) {
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
    
    public List<RutaOptimaDTO.NodoDTO> getNodos() {
        return nodos;
    }
    
    public void setNodos(List<RutaOptimaDTO.NodoDTO> nodos) {
        this.nodos = nodos;
    }
    
    public List<RutaOptimaDTO.SegmentoRuta> getSegmentos() {
        return segmentos;
    }
    
    public void setSegmentos(List<RutaOptimaDTO.SegmentoRuta> segmentos) {
        this.segmentos = segmentos;
    }
    
    public List<String> getInstrucciones() {
        return instrucciones;
    }
    
    public void setInstrucciones(List<String> instrucciones) {
        this.instrucciones = instrucciones;
    }
    
    public String getVisibilidad() {
        return visibilidad;
    }
    
    public void setVisibilidad(String visibilidad) {
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
    public Long getRestauranteId() { return restauranteId; }
public void setRestauranteId(Long restauranteId) { this.restauranteId = restauranteId; }

public String getRestauranteNombre() { return restauranteNombre; }
public void setRestauranteNombre(String restauranteNombre) { this.restauranteNombre = restauranteNombre; }

public Long getRepartidorId() { return repartidorId; }
public void setRepartidorId(Long repartidorId) { this.repartidorId = repartidorId; }

public String getRepartidorNombre() { return repartidorNombre; }
public void setRepartidorNombre(String repartidorNombre) { this.repartidorNombre = repartidorNombre; }

public Long getNodoClienteId() { return nodoClienteId; }
public void setNodoClienteId(Long nodoClienteId) { this.nodoClienteId = nodoClienteId; }

public Long getNodoRepartidorId() { return nodoRepartidorId; }
public void setNodoRepartidorId(Long nodoRepartidorId) { this.nodoRepartidorId = nodoRepartidorId; }

public String getNodosRutaJson() { return nodosRutaJson; }
public void setNodosRutaJson(String nodosRutaJson) { this.nodosRutaJson = nodosRutaJson; }

public String getSegmentosRutaJson() { return segmentosRutaJson; }
public void setSegmentosRutaJson(String segmentosRutaJson) { this.segmentosRutaJson = segmentosRutaJson; }

public String getInstruccionesJson() { return instruccionesJson; }
public void setInstruccionesJson(String instruccionesJson) { this.instruccionesJson = instruccionesJson; }


}