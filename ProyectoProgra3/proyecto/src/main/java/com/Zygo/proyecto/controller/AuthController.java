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

// ⭐ IMPORTS DE SWAGGER - ¡ESTOS SON NECESARIOS!
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "1. 🔐 Autenticación", description = "Endpoints para login, registro y gestión de sesiones de usuarios")
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
    
    @Operation(
        summary = "Login de usuario",
        description = "Autentica un usuario (cliente, repartidor o admin) y devuelve un token JWT válido por 24 horas"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Login exitoso - Token JWT generado",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Credenciales inválidas - Email o contraseña incorrectos"
        )
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("🔑 Intento de login para: {}", loginRequest.getEmail());
        
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
            
            log.info("✅ Login exitoso para: {} con rol: {}", loginRequest.getEmail(), usuario.getTipo());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error en login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Email o contraseña incorrectos");
        }
    }
    
    @Operation(
        summary = "Registrar nuevo usuario",
        description = "Crea una nueva cuenta de usuario. Solo ADMIN puede crear otros ADMIN"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201", 
            description = "Usuario registrado exitosamente",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Email ya registrado o datos inválidos"
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Sin permisos para crear usuarios ADMIN"
        )
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("📝 Intento de registro para: {} como {}", 
                registerRequest.getEmail(), registerRequest.getTipo());
        
        try {
            if (usuarioRepository.existsByEmail(registerRequest.getEmail())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("El email ya está registrado");
            }
            
            // 🔒 Validación: Solo ADMIN puede crear otros ADMIN
            if (registerRequest.getTipo() == Usuario.TipoUsuario.ADMIN) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                    log.warn("⚠️ Intento de crear ADMIN sin permisos desde: {}", registerRequest.getEmail());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("No tienes permisos para crear usuarios administradores");
                }
            }
            
            Usuario usuario = new Usuario();
            usuario.setNombre(registerRequest.getNombre());
            usuario.setEmail(registerRequest.getEmail());
            usuario.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            usuario.setTelefono(registerRequest.getTelefono());
            usuario.setDireccion(registerRequest.getDireccion());
            usuario.setTipo(registerRequest.getTipo());
            usuario.setActivo(true);
            
            if (registerRequest.getTipo() == Usuario.TipoUsuario.REPARTIDOR) {
                usuario.setDisponible(true);
            }
            
            Usuario savedUsuario = usuarioRepository.save(usuario);
            
            String jwt = jwtUtil.generarTokenFromEmailAndRole(
                    savedUsuario.getEmail(), 
                    savedUsuario.getTipo().name()
            );
            
            LoginResponse response = new LoginResponse(
                    jwt,
                    savedUsuario.getId(),
                    savedUsuario.getNombre(),
                    savedUsuario.getEmail(),
                    savedUsuario.getTipo()
            );
            
            log.info("✅ Registro exitoso para: {} como {}", 
                    registerRequest.getEmail(), registerRequest.getTipo());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("❌ Error en registro: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al registrar usuario: " + e.getMessage());
        }
    }
    
    @Operation(
        summary = "Obtener usuario actual",
        description = "Retorna información del usuario autenticado basándose en el token JWT"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Usuario encontrado",
            content = @Content(schema = @Schema(implementation = Usuario.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Token inválido o expirado"
        )
    })
    @GetMapping("/me")
    public ResponseEntity<?> obtenerUsuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        log.info("👤 Usuario actual: {} - Rol: {}", usuario.getNombre(), usuario.getTipo());
        return ResponseEntity.ok(usuario);
    }
    
    @Operation(
        summary = "Verificar rol del usuario",
        description = "Retorna el tipo de usuario y sus authorities basándose en el token JWT"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rol verificado exitosamente")
    })
    @GetMapping("/check-role")
    public ResponseEntity<?> verificarRol() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        return ResponseEntity.ok(new RoleCheckResponse(
                usuario.getTipo().name(),
                authentication.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .toList()
        ));
    }
    
    private record RoleCheckResponse(String tipoUsuario, java.util.List<String> authorities) {}
}