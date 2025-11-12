package com.Zygo.proyecto.dto;

import com.Zygo.proyecto.model.Usuario.TipoUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RegisterRequest {
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    private String email;
    
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
    
    @NotBlank(message = "El teléfono es obligatorio")
    private String telefono;
    
    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;
    
    @NotNull(message = "El tipo de usuario es obligatorio")
    private TipoUsuario tipo;
    
    public RegisterRequest() {}
    
    public RegisterRequest(String nombre, String email, String password, String telefono, 
                          String direccion, TipoUsuario tipo) {
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.telefono = telefono;
        this.direccion = direccion;
        this.tipo = tipo;
    }
    
    // Getters y Setters
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
}