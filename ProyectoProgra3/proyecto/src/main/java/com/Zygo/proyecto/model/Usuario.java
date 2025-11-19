package com.Zygo.proyecto.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String nombre;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String password;  // NUEVO CAMPO
    
    @Column(nullable = false)
    private String telefono;
    
    @Column(nullable = false)
    private String direccion;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoUsuario tipo;
    
    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;
    
    @Column(nullable = false)
    private Boolean activo = true;
    // Agregar DESPUÉS de la línea "private String direccion;"

    // Coordenadas para ubicación en tiempo real (especialmente repartidores)
    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    // Disponibilidad del repartidor
    @Column(name = "disponible")
    private Boolean disponible = true;
    
    @PrePersist
    protected void onCreate() {
        fechaRegistro = LocalDateTime.now();
    }
    
    public enum TipoUsuario {
        CLIENTE,
        REPARTIDOR,
        ADMIN  // NUEVO ROL
    }
    
    // Constructores
    public Usuario() {}
    
    public Usuario(Long id, String nombre, String email, String password, String telefono, 
                   String direccion, TipoUsuario tipo, LocalDateTime fechaRegistro, Boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.telefono = telefono;
        this.direccion = direccion;
        this.tipo = tipo;
        this.fechaRegistro = fechaRegistro;
        this.activo = activo;
    }
    
    // Getters y Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getTelefono() {
        return telefono;
    }
    
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
    
    public String getDireccion() {
        return direccion;
    }
    
    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
    
    public TipoUsuario getTipo() {
        return tipo;
    }
    
    public void setTipo(TipoUsuario tipo) {
        this.tipo = tipo;
    }
    
    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }
    
    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
    
    public Boolean getActivo() {
        return activo;
    }
    
    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
    public Double getLatitud() {
    return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    public Boolean getDisponible() {
        return disponible;
    }

    public void setDisponible(Boolean disponible) {
        this.disponible = disponible;
    }
}