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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 🔥 Habilita @PreAuthorize en controladores
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
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Requested-With",
            "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"
        ));
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            .authorizeHttpRequests(auth -> auth
                // 🟢 PÚBLICOS - Sin autenticación
                .requestMatchers(
                    "/api/auth/**",
                    "/actuator/health",
                    "/error"
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
            );
        
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}