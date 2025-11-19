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

import java.util.Arrays;
import java.util.List;

/**
 * 🔒 CONFIGURACIÓN DE SEGURIDAD CORREGIDA
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
     * ✅ CONFIGURACIÓN CORS CORREGIDA
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // ✅ Permitir todos los orígenes en desarrollo
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // ✅ Métodos HTTP permitidos
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));
        
        // ✅ Headers permitidos
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // ✅ Permitir credenciales
        configuration.setAllowCredentials(true);
        
        // ✅ Headers expuestos
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));
        
        // ✅ Cache de preflight (1 hora)
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    /**
     * ✅ CADENA DE FILTROS DE SEGURIDAD CORREGIDA
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ✅ Habilitar CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // ✅ Deshabilitar CSRF (usamos JWT)
            .csrf(csrf -> csrf.disable())
            
            // ✅ Sin estado (stateless) - JWT
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // ✅ AUTORIZACIÓN DE ENDPOINTS
            .authorizeHttpRequests(auth -> auth
                // 🟢 PÚBLICOS - Sin autenticación
                .requestMatchers(
                    "/api/auth/**",
                    "/api/auth/login",
                    "/api/auth/register",
                    "/actuator/health",
                    "/error"
                ).permitAll()
                
                // 🟢 Endpoints públicos de pedidos
                .requestMatchers(
                    "/api/pedidos/crear-con-asignacion",
                    "/api/pedidos/estadisticas/**"
                ).permitAll()
                
                // 🟢 Endpoints públicos de rutas y lugares
                .requestMatchers(
                    "/api/rutas/**",
                    "/api/lugares/**"
                ).permitAll()
                
                // 🟢 Endpoints públicos del admin para mapa
                .requestMatchers(
                    "/api/admin/estadisticas",
                    "/api/admin/mapa/**"
                ).permitAll()
                
                // 🟢 Recursos estáticos
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/mapa.html",
                    "/static/**",
                    "/*.html",
                    "/*.css",
                    "/*.js",
                    "/assets/**"
                ).permitAll()
                
                // 🔴 ADMIN - Solo administradores
                .requestMatchers("/api/admin/**")
                    .hasAuthority("ROLE_ADMIN")
                
                // 🟡 USUARIOS - Todos los roles autenticados
                .requestMatchers("/api/usuarios/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_CLIENTE", "ROLE_REPARTIDOR")
                
                // 🟡 PEDIDOS - Todos los roles autenticados
                .requestMatchers("/api/pedidos/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_CLIENTE", "ROLE_REPARTIDOR")
                
                // 🟡 REPARTIDOR - Solo repartidores y admins
                .requestMatchers("/api/repartidor/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_REPARTIDOR")
                
                // 🔴 Todo lo demás requiere autenticación
                .anyRequest().authenticated()
            );
        
        // ✅ Configurar provider de autenticación
        http.authenticationProvider(authenticationProvider());
        
        // ✅ Agregar filtro JWT ANTES del filtro de autenticación estándar
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}