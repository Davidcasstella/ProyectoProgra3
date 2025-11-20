package com.Zygo.proyecto.controller;

import com.Zygo.proyecto.dto.UsuarioDTO;
import com.Zygo.proyecto.service.UsuarioService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    
    private static final Logger log = LoggerFactory.getLogger(UsuarioController.class);
    
    @Autowired
    private UsuarioService usuarioService;
    
    @PostMapping
    public ResponseEntity<UsuarioDTO> crearUsuario(@Valid @RequestBody UsuarioDTO usuarioDTO) {
        log.info("POST /api/usuarios - Creando usuario");
        UsuarioDTO creado = usuarioService.crearUsuario(usuarioDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> obtenerUsuario(@PathVariable Long id) {
        log.info("GET /api/usuarios/{} - Obteniendo usuario", id);
        UsuarioDTO usuario = usuarioService.obtenerUsuarioPorId(id);
        return ResponseEntity.ok(usuario);
    }
    
    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> obtenerTodosLosUsuarios() {
        log.info("GET /api/usuarios - Obteniendo todos los usuarios");
        List<UsuarioDTO> usuarios = usuarioService.obtenerTodosLosUsuarios();
        return ResponseEntity.ok(usuarios);
    }
    
    // ✅ NUEVO: Obtener repartidores CON ubicación
    @GetMapping("/repartidores")
    public ResponseEntity<List<UsuarioDTO>> obtenerRepartidores() {
        log.info("GET /api/usuarios/repartidores - Obteniendo repartidores con ubicación");
        List<UsuarioDTO> repartidores = usuarioService.obtenerRepartidoresConUbicacion();
        return ResponseEntity.ok(repartidores);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> actualizarUsuario(
            @PathVariable Long id, 
            @Valid @RequestBody UsuarioDTO usuarioDTO) {
        log.info("PUT /api/usuarios/{} - Actualizando usuario", id);
        UsuarioDTO actualizado = usuarioService.actualizarUsuario(id, usuarioDTO);
        return ResponseEntity.ok(actualizado);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        log.info("DELETE /api/usuarios/{} - Eliminando usuario", id);
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleException(RuntimeException ex) {
        log.error("Error en UsuarioController: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}