package com.Zygo.proyecto.dto;

import com.Zygo.proyecto.model.Pedido.EstadoPedido;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public class PedidoDTO {
    
    private Long id;
    
    @NotNull(message = "El ID del cliente es obligatorio")
    private Long clienteId;
    
    private Long repartidorId;
    
    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;
    
    @NotBlank(message = "La dirección de origen es obligatoria")
    private String direccionOrigen;
    
    @NotBlank(message = "La dirección de destino es obligatoria")
    private String direccionDestino;
    
    @NotNull(message = "La distancia es obligatoria")
    @Positive(message = "La distancia debe ser positiva")
    private Double distanciaKm;
    
    private Double costo;
    
    private EstadoPedido estado;
    
    private LocalDateTime fechaCreacion;
    
    private LocalDateTime fechaAsignacion;
    
    private LocalDateTime fechaEntrega;
    
    // Información adicional para respuestas
    private String nombreCliente;
    private String nombreRepartidor;
    
    // Constructores
    public PedidoDTO() {}
    
    public PedidoDTO(Long id, Long clienteId, Long repartidorId, String descripcion, 
                     String direccionOrigen, String direccionDestino, Double distanciaKm, 
                     Double costo, EstadoPedido estado, LocalDateTime fechaCreacion, 
                     LocalDateTime fechaAsignacion, LocalDateTime fechaEntrega, 
                     String nombreCliente, String nombreRepartidor) {
        this.id = id;
        this.clienteId = clienteId;
        this.repartidorId = repartidorId;
        this.descripcion = descripcion;
        this.direccionOrigen = direccionOrigen;
        this.direccionDestino = direccionDestino;
        this.distanciaKm = distanciaKm;
        this.costo = costo;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.fechaAsignacion = fechaAsignacion;
        this.fechaEntrega = fechaEntrega;
        this.nombreCliente = nombreCliente;
        this.nombreRepartidor = nombreRepartidor;
    }
    
    // Getters y Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getClienteId() {
        return clienteId;
    }
    
    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }
    
    public Long getRepartidorId() {
        return repartidorId;
    }
    
    public void setRepartidorId(Long repartidorId) {
        this.repartidorId = repartidorId;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDireccionOrigen() {
        return direccionOrigen;
    }
    
    public void setDireccionOrigen(String direccionOrigen) {
        this.direccionOrigen = direccionOrigen;
    }
    
    public String getDireccionDestino() {
        return direccionDestino;
    }
    
    public void setDireccionDestino(String direccionDestino) {
        this.direccionDestino = direccionDestino;
    }
    
    public Double getDistanciaKm() {
        return distanciaKm;
    }
    
    public void setDistanciaKm(Double distanciaKm) {
        this.distanciaKm = distanciaKm;
    }
    
    public Double getCosto() {
        return costo;
    }
    
    public void setCosto(Double costo) {
        this.costo = costo;
    }
    
    public EstadoPedido getEstado() {
        return estado;
    }
    
    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }
    
    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
    
    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
    
    public LocalDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }
    
    public void setFechaAsignacion(LocalDateTime fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }
    
    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }
    
    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }
    
    public String getNombreCliente() {
        return nombreCliente;
    }
    
    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }
    
    public String getNombreRepartidor() {
        return nombreRepartidor;
    }
    
    public void setNombreRepartidor(String nombreRepartidor) {
        this.nombreRepartidor = nombreRepartidor;
    }
}