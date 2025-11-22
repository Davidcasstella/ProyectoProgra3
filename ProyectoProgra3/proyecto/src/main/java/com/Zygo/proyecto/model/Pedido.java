package com.Zygo.proyecto.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// ✅ IMPORTS CORRECTOS para el Logger
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "pedidos")
public class Pedido {
    // ✅ Logger correcto usando SLF4J
    private static final Logger log = LoggerFactory.getLogger(Pedido.class);
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;
    
    @ManyToOne
    @JoinColumn(name = "repartidor_id")
    private Usuario repartidor;
    
    // Restaurante asignado automáticamente
    @ManyToOne
    @JoinColumn(name = "restaurante_id")
    private Graph restaurante;

    // Nodo del grafo más cercano al cliente
    @ManyToOne
    @JoinColumn(name = "nodo_cliente_id")
    private Graph nodoCliente;

    // Nodo del grafo donde está el repartidor
    @ManyToOne
    @JoinColumn(name = "nodo_repartidor_id")
    private Graph nodoRepartidor;
    
    @Column(nullable = false)
    private String descripcion;
    
    @Column(name = "direccion_origen", nullable = false)
    private String direccionOrigen;
    
    @Column(name = "direccion_destino", nullable = false)
    private String direccionDestino;
    
    @Column(nullable = false)
    private Double distanciaKm;
    
    @Column(nullable = false)
    private Double costo;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedido estado;
    
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
    
    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion;
    
    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    // ================== COORDENADAS ==================
    
    // 📍 ORIGEN (Repartidor)
    @Column(name = "lat_origen")
    private Double latOrigen;

    @Column(name = "lon_origen")
    private Double lonOrigen;

    // 🍽️ RESTAURANTE (NUEVO)
    @Column(name = "lat_restaurante")
    private Double latRestaurante;

    @Column(name = "lon_restaurante")
    private Double lonRestaurante;

    @Column(name = "direccion_restaurante", length = 500)
    private String direccionRestaurante;

    @Column(name = "nombre_restaurante", length = 200)
    private String nombreRestaurante;

    // 🎯 DESTINO (Cliente)
    @Column(name = "lat_destino")
    private Double latDestino;

    @Column(name = "lon_destino")
    private Double lonDestino;
    
    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        
        // ✅ Solo establecer PENDIENTE si no viene estado
        if (estado == null) {
            estado = EstadoPedido.PENDIENTE;
            log.info("Estado establecido por defecto: PENDIENTE");
        } else {
            log.info("✅ Estado personalizado conservado: {}", estado);
        }
    }
    
    public enum EstadoPedido {
        PENDIENTE,
        ASIGNADO,
        EN_CAMINO,
        ENTREGADO,
        CANCELADO
    }
    
    // Constructores
    public Pedido() {}
    
    public Pedido(Long id, Usuario cliente, Usuario repartidor, String descripcion, 
                  String direccionOrigen, String direccionDestino, Double distanciaKm, 
                  Double costo, EstadoPedido estado, LocalDateTime fechaCreacion, 
                  LocalDateTime fechaAsignacion, LocalDateTime fechaEntrega) {
        this.id = id;
        this.cliente = cliente;
        this.repartidor = repartidor;
        this.descripcion = descripcion;
        this.direccionOrigen = direccionOrigen;
        this.direccionDestino = direccionDestino;
        this.distanciaKm = distanciaKm;
        this.costo = costo;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.fechaAsignacion = fechaAsignacion;
        this.fechaEntrega = fechaEntrega;
    }
    
    // ================== GETTERS Y SETTERS ==================
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Usuario getCliente() {
        return cliente;
    }
    
    public void setCliente(Usuario cliente) {
        this.cliente = cliente;
    }
    
    public Usuario getRepartidor() {
        return repartidor;
    }
    
    public void setRepartidor(Usuario repartidor) {
        this.repartidor = repartidor;
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
    
    // Getters y Setters para ORIGEN (Repartidor)
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

    // 🆕 Getters y Setters para RESTAURANTE
    public Double getLatRestaurante() {
        return latRestaurante;
    }

    public void setLatRestaurante(Double latRestaurante) {
        this.latRestaurante = latRestaurante;
    }

    public Double getLonRestaurante() {
        return lonRestaurante;
    }

    public void setLonRestaurante(Double lonRestaurante) {
        this.lonRestaurante = lonRestaurante;
    }

    public String getDireccionRestaurante() {
        return direccionRestaurante;
    }

    public void setDireccionRestaurante(String direccionRestaurante) {
        this.direccionRestaurante = direccionRestaurante;
    }

    public String getNombreRestaurante() {
        return nombreRestaurante;
    }

    public void setNombreRestaurante(String nombreRestaurante) {
        this.nombreRestaurante = nombreRestaurante;
    }

    // Getters y Setters para DESTINO (Cliente)
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
    
    // Getters y Setters para nodos y restaurante
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
}