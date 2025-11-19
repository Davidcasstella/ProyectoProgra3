package com.Zygo.proyecto.config;

import com.Zygo.proyecto.security.CustomUserDetailsService;
import com.Zygo.proyecto.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

import java.util.Arrays;
import java.util.List;

/**
 * 🔒 CONFIGURACIÓN DE SEGURIDAD
 * 
 * Maneja:
 * - Autenticación JWT
 * - CORS para frontend
 * - Autorización por roles
 * - Endpoints públicos vs protegidos
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    /**
     * ✅ CONFIGURACIÓN CORS
     * Permite que el frontend (Angular) se comunique con el backend
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // ✅ Orígenes permitidos
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:4200",      // Angular dev server
            "http://localhost:*",         // Cualquier puerto local
            "http://127.0.0.1:4200",      // IPv4 local
            "http://192.168.*.*:*"        // Red local
        ));
        
        // ✅ Métodos HTTP permitidos
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        
        // ✅ Headers permitidos (incluyendo Authorization)
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        
        // ✅ Headers expuestos en respuesta
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Access-Control-Allow-Origin"
        ));
        
        // ✅ Tiempo de caché de preflight
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    /**
     * ✅ CADENA DE FILTROS DE SEGURIDAD
     * 
     * Define qué endpoints son públicos y cuáles requieren autenticación
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Deshabilitar CSRF (usamos JWT en su lugar)
            .csrf(csrf -> csrf.disable())
            
            // Stateless - sin sesiones (JWT)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // ✅ CONFIGURACIÓN DE AUTORIZACIÓN
            .authorizeHttpRequests(auth -> auth
                
                // 🟢 ENDPOINTS PÚBLICOS (sin autenticación)
                .requestMatchers("/api/auth/**").permitAll()
                
                // ⭐ CORREGIDO: Usar HttpMethod en lugar de strings
                .requestMatchers(HttpMethod.POST, "/api/pedidos/crear-con-asignacion").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/pedidos/estadisticas/**").permitAll()
                
                .requestMatchers("/api/rutas/**").permitAll()
                .requestMatchers("/api/lugares/**").permitAll()
                .requestMatchers("/api/admin/estadisticas").permitAll()
                .requestMatchers("/api/admin/mapa/**").permitAll()
                .requestMatchers("/", "/index.html", "/mapa.html", "/static/**", "/*.html").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                
                // 🟠 ENDPOINTS PROTEGIDOS - Solo ADMIN
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                
                // 🟠 ENDPOINTS PROTEGIDOS - Clientes, Repartidores, Admin
                .requestMatchers("/api/usuarios/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_CLIENTE", "ROLE_REPARTIDOR")
                .requestMatchers("/api/pedidos/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_CLIENTE", "ROLE_REPARTIDOR")
                .requestMatchers("/api/repartidor/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_REPARTIDOR")
                
                // ⚠️ Rechazar todo lo demás
                .anyRequest().authenticated()
            );
        
        // Proveedores de autenticación
        http.authenticationProvider(authenticationProvider());
        
        // Filtro JWT
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}