package com.Zygo.proyecto.dto;

import com.Zygo.proyecto.model.Usuario.TipoUsuario;

public class LoginResponse {
    
    private String token;
    private String tipo = "Bearer";
    private Long id;
    private String nombre;
    private String email;
    private TipoUsuario tipoUsuario;
    
    public LoginResponse() {}
    
    public LoginResponse(String token, Long id, String nombre, String email, TipoUsuario tipoUsuario) {
        this.token = token;
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.tipoUsuario = tipoUsuario;
    }
    
    // Getters y Setters
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getTipo() {
        return tipo;
    }
    
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
    
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
    
    public TipoUsuario getTipoUsuario() {
        return tipoUsuario;
    }
    
    public void setTipoUsuario(TipoUsuario tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }
}