package com.Zygo.proyecto.controller;

import com.Zygo.proyecto.dto.LoginRequest;
import com.Zygo.proyecto.dto.LoginResponse;
import com.Zygo.proyecto.dto.RegisterRequest;
import com.Zygo.proyecto.model.Usuario;
import com.Zygo.proyecto.repository.UsuarioRepository;
import com.Zygo.proyecto.security.JwtUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Intento de login para: {}", loginRequest.getEmail());
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtil.generarToken(authentication);
            
            Usuario usuario = usuarioRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            LoginResponse response = new LoginResponse(
                    jwt,
                    usuario.getId(),
                    usuario.getNombre(),
                    usuario.getEmail(),
                    usuario.getTipo()
            );
            
            log.info("Login exitoso para: {}", loginRequest.getEmail());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error en login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Email o contraseña incorrectos");
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Intento de registro para: {}", registerRequest.getEmail());
        
        try {
            if (usuarioRepository.existsByEmail(registerRequest.getEmail())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("El email ya está registrado");
            }
            
            Usuario usuario = new Usuario();
            usuario.setNombre(registerRequest.getNombre());
            usuario.setEmail(registerRequest.getEmail());
            usuario.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            usuario.setTelefono(registerRequest.getTelefono());
            usuario.setDireccion(registerRequest.getDireccion());
            usuario.setTipo(registerRequest.getTipo());
            usuario.setActivo(true);
            
            Usuario savedUsuario = usuarioRepository.save(usuario);
            
            String jwt = jwtUtil.generarTokenFromEmail(savedUsuario.getEmail());
            
            LoginResponse response = new LoginResponse(
                    jwt,
                    savedUsuario.getId(),
                    savedUsuario.getNombre(),
                    savedUsuario.getEmail(),
                    savedUsuario.getTipo()
            );
            
            log.info("Registro exitoso para: {}", registerRequest.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Error en registro: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al registrar usuario");
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> obtenerUsuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        return ResponseEntity.ok(usuario);
    }
}