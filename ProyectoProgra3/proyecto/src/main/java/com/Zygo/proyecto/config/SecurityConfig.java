package com.Zygo.proyecto.config;

import com.Zygo.proyecto.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // 🟢 PÚBLICOS - Sin autenticación
                .requestMatchers(
                    "/api/auth/**",
                    "/actuator/health",
                    "/error"
                ).permitAll()
                
                // 📚 SWAGGER/OpenAPI - Acceso público a documentación
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()
                
                // 🟢 Recursos estáticos
                .requestMatchers(
                    "/", "/index.html", "/mapa.html",
                    "/static/**", "/*.html", "/*.css", "/*.js",
                    "/assets/**"
                ).permitAll()
                
                // 🔴 ADMIN - Solo administradores
                .requestMatchers("/api/admin/**")
                    .hasAuthority("ROLE_ADMIN")
                
                // 🔵 CLIENTE - Solo clientes
                .requestMatchers("/api/cliente/**")
                    .hasAuthority("ROLE_CLIENTE")
                
                // 🟡 REPARTIDOR - Solo repartidores
                .requestMatchers("/api/repartidor/**")
                    .hasAuthority("ROLE_REPARTIDOR")
                
                // 🟣 USUARIOS - Admin puede gestionar
                .requestMatchers("/api/usuarios/**")
                    .hasAuthority("ROLE_ADMIN")
                
                // 🟠 PEDIDOS - Acceso diferenciado
                .requestMatchers("/api/pedidos/crear-con-asignacion")
                    .permitAll()  // Puede ser público o requerir CLIENTE
                .requestMatchers("/api/pedidos/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_CLIENTE", "ROLE_REPARTIDOR")
                
                // 🗺️ RUTAS Y LUGARES - Todos los autenticados
                .requestMatchers("/api/rutas/**", "/api/lugares/**")
                    .authenticated()
                
                // 🔴 Todo lo demás requiere autenticación
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}