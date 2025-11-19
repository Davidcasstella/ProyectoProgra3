package com.Zygo.proyecto.dto;

import com.Zygo.proyecto.model.Pedido;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

/**
 * 📦 DTO para transferencia de datos de Pedidos
 * Usa records internos para información de cliente y repartidor
 */
public class PedidoDTO {
    
    private Long id;
    
    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;
    
    @NotBlank(message = "La dirección de origen es obligatoria")
    private String direccionOrigen;
    
    @NotBlank(message = "La dirección de destino es obligatoria")
    private String direccionDestino;
    
    @Positive(message = "La distancia debe ser positiva")
    private Double distanciaKm;
    
    private Double costo;
    private Pedido.EstadoPedido estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaAsignacion;
    private LocalDateTime fechaEntrega;
    
    // Información básica del cliente y repartidor
    private ClienteInfo cliente;
    private RepartidorInfo repartidor;
    
    // Coordenadas GPS
    private Double latOrigen;
    private Double lonOrigen;
    private Double latDestino;
    private Double lonDestino;
    
    // ===== CONSTRUCTORES =====
    public PedidoDTO() {}
    
    public PedidoDTO(Pedido pedido) {
        this.id = pedido.getId();
        this.descripcion = pedido.getDescripcion();
        this.direccionOrigen = pedido.getDireccionOrigen();
        this.direccionDestino = pedido.getDireccionDestino();
        this.distanciaKm = pedido.getDistanciaKm();
        this.costo = pedido.getCosto();
        this.estado = pedido.getEstado();
        this.fechaCreacion = pedido.getFechaCreacion();
        this.fechaAsignacion = pedido.getFechaAsignacion();
        this.fechaEntrega = pedido.getFechaEntrega();
        
        if (pedido.getCliente() != null) {
            this.cliente = new ClienteInfo(
                pedido.getCliente().getId(),
                pedido.getCliente().getNombre(),
                pedido.getCliente().getTelefono()
            );
        }
        
        if (pedido.getRepartidor() != null) {
            this.repartidor = new RepartidorInfo(
                pedido.getRepartidor().getId(),
                pedido.getRepartidor().getNombre(),
                pedido.getRepartidor().getTelefono()
            );
        }
        
        this.latOrigen = pedido.getLatOrigen();
        this.lonOrigen = pedido.getLonOrigen();
        this.latDestino = pedido.getLatDestino();
        this.lonDestino = pedido.getLonDestino();
    }
    
    // ===== RECORDS INTERNOS =====
    public record ClienteInfo(Long id, String nombre, String telefono) {}
    public record RepartidorInfo(Long id, String nombre, String telefono) {}
    
    // ===== MÉTODOS AUXILIARES PARA COMPATIBILIDAD CON SERVICIOS =====
    
    /**
     * 🔧 Obtener ID del cliente (compatibilidad)
     */
    public Long getClienteId() {
        return cliente != null ? cliente.id() : null;
    }
    
    /**
     * 🔧 Establecer ID del cliente (compatibilidad)
     */
    public void setClienteId(Long clienteId) {
        if (clienteId != null) {
            this.cliente = new ClienteInfo(clienteId, null, null);
        }
    }
    
    /**
     * 🔧 Obtener nombre del cliente (compatibilidad)
     */
    public String getNombreCliente() {
        return cliente != null ? cliente.nombre() : null;
    }
    
    /**
     * 🔧 Establecer nombre del cliente (compatibilidad)
     */
    public void setNombreCliente(String nombre) {
        if (cliente != null) {
            this.cliente = new ClienteInfo(cliente.id(), nombre, cliente.telefono());
        } else {
            this.cliente = new ClienteInfo(null, nombre, null);
        }
    }
    
    /**
     * 🔧 Obtener ID del repartidor (compatibilidad)
     */
    public Long getRepartidorId() {
        return repartidor != null ? repartidor.id() : null;
    }
    
    /**
     * 🔧 Establecer ID del repartidor (compatibilidad)
     */
    public void setRepartidorId(Long repartidorId) {
        if (repartidorId != null) {
            this.repartidor = new RepartidorInfo(repartidorId, null, null);
        } else {
            this.repartidor = null;
        }
    }
    
    /**
     * 🔧 Obtener nombre del repartidor (compatibilidad)
     */
    public String getNombreRepartidor() {
        return repartidor != null ? repartidor.nombre() : null;
    }
    
    /**
     * 🔧 Establecer nombre del repartidor (compatibilidad)
     */
    public void setNombreRepartidor(String nombre) {
        if (repartidor != null) {
            this.repartidor = new RepartidorInfo(repartidor.id(), nombre, repartidor.telefono());
        } else {
            this.repartidor = new RepartidorInfo(null, nombre, null);
        }
    }
    
    // ===== GETTERS Y SETTERS ESTÁNDAR =====
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Pedido.EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(Pedido.EstadoPedido estado) {
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

    public ClienteInfo getCliente() {
        return cliente;
    }

    public void setCliente(ClienteInfo cliente) {
        this.cliente = cliente;
    }

    public RepartidorInfo getRepartidor() {
        return repartidor;
    }

    public void setRepartidor(RepartidorInfo repartidor) {
        this.repartidor = repartidor;
    }

    public Double getLatOrigen() {
        return latOrigen;
    }

    public void setLatOrigen(Double latOrigen) {
        this.latOrigen = latOrigen;
    }

    public Double getLonOrigen() {
        return lonOrigen;
    }

    public void setLonOrigen(Double lonOrigen) {
        this.lonOrigen = lonOrigen;
    }

    public Double getLatDestino() {
        return latDestino;
    }

    public void setLatDestino(Double latDestino) {
        this.latDestino = latDestino;
    }

    public Double getLonDestino() {
        return lonDestino;
    }

    public void setLonDestino(Double lonDestino) {
        this.lonDestino = lonDestino;
    }
    
    @Override
    public String toString() {
        return "PedidoDTO{" +
                "id=" + id +
                ", clienteId=" + getClienteId() +
                ", repartidorId=" + getRepartidorId() +
                ", estado=" + estado +
                ", distancia=" + distanciaKm + "km" +
                ", costo=" + costo +
                '}';
    }
}