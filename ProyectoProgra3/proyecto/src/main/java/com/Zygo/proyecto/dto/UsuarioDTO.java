package com.Zygo.proyecto.dto;

import com.Zygo.proyecto.model.Usuario.TipoUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class UsuarioDTO {
    // ✅ NUEVOS CAMPOS para ubicación
private Double latitud;
private Double longitud;
private Boolean disponible;

    
    private Long id;
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    private String email;
    
    @NotBlank(message = "El teléfono es obligatorio")
    private String telefono;
    
    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;
    
    @NotNull(message = "El tipo de usuario es obligatorio")
    private TipoUsuario tipo;
    
    private LocalDateTime fechaRegistro;
    
    private Boolean activo;
    
    // Constructores
    public UsuarioDTO() {}
    
    public UsuarioDTO(Long id, String nombre, String email, String telefono, String direccion, 
                      TipoUsuario tipo, LocalDateTime fechaRegistro, Boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
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
    // ✅ GETTERS Y SETTERS
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