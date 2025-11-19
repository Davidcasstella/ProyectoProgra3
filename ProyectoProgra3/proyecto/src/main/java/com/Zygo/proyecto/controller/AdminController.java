package com.Zygo.proyecto.controller;

import com.Zygo.proyecto.model.Graph;
import com.Zygo.proyecto.model.Usuario;
import com.Zygo.proyecto.repository.GraphRepository;
import com.Zygo.proyecto.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {
    
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    
    @Autowired
    private GraphRepository graphRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * Endpoint para crear los 7 restaurantes
     */
    @PostMapping("/inicializar-restaurantes")
    public ResponseEntity<?> inicializarRestaurantes() {
        try {
            long restaurantesExistentes = graphRepository.countByTipo(Graph.TipoNodo.RESTAURANTE);
            
            if (restaurantesExistentes >= 7) {
                return ResponseEntity.ok(Map.of(
                    "mensaje", "Ya existen " + restaurantesExistentes + " restaurantes",
                    "restaurantes", restaurantesExistentes
                ));
            }
            
            log.info("🍽️ Creando 7 restaurantes...");
            
            String[][] datosRestaurantes = {
                {"Restaurante El Buen Sabor", "5.7150", "-72.9350", "Cra 10 #15-25"},
                {"Pizza Express", "5.7180", "-72.9320", "Calle 12 #8-45"},
                {"Hamburguesas Gourmet", "5.7120", "-72.9380", "Av. Circunvalar #20-10"},
                {"Comidas Rápidas Central", "5.7160", "-72.9340", "Cra 11 #13-30"},
                {"Asados Don Pepe", "5.7140", "-72.9360", "Calle 14 #9-20"},
                {"Restaurante La Plaza", "5.7170", "-72.9330", "Cra 9 #12-15"},
                {"Parrilla del Norte", "5.7130", "-72.9370", "Av. Suarez Rendon #18-40"}
            };
            
            for (String[] datos : datosRestaurantes) {
                Graph restaurante = new Graph();
                restaurante.setNombre(datos[0]);
                restaurante.setLatitud(Double.parseDouble(datos[1]));
                restaurante.setLongitud(Double.parseDouble(datos[2]));
                restaurante.setTipo(Graph.TipoNodo.RESTAURANTE);
                restaurante.setDireccionCompleta(datos[3]);
                restaurante.setActivo(true);
                restaurante.setDescripcion("Restaurante disponible para pedidos de delivery");
                graphRepository.save(restaurante);
            }
            
            log.info("✅ 7 restaurantes creados exitosamente");
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "Restaurantes creados exitosamente",
                "restaurantes", 7
            ));
            
        } catch (Exception e) {
            log.error("❌ Error creando restaurantes: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Error creando restaurantes: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Endpoint para crear los 15 repartidores
     */
    @PostMapping("/inicializar-repartidores")
    public ResponseEntity<?> inicializarRepartidores() {
        try {
            long repartidoresExistentes = usuarioRepository.countByTipo(Usuario.TipoUsuario.REPARTIDOR);
            
            if (repartidoresExistentes >= 15) {
                return ResponseEntity.ok(Map.of(
                    "mensaje", "Ya existen " + repartidoresExistentes + " repartidores",
                    "repartidores", repartidoresExistentes
                ));
            }
            
            log.info("🚴 Creando repartidores faltantes...");
            
            String[][] datosRepartidores = {
                {"Juan Pérez", "repartidor1@zygo.com", "5.7155", "-72.9355", "3101111111"},
                {"María López", "repartidor2@zygo.com", "5.7165", "-72.9345", "3102222222"},
                {"Pedro García", "repartidor3@zygo.com", "5.7135", "-72.9365", "3103333333"},
                {"Ana Rodríguez", "repartidor4@zygo.com", "5.7175", "-72.9335", "3104444444"},
                {"Luis Martínez", "repartidor5@zygo.com", "5.7125", "-72.9375", "3105555555"},
                {"Carmen Sánchez", "repartidor6@zygo.com", "5.7160", "-72.9340", "3106666666"},
                {"Diego Torres", "repartidor7@zygo.com", "5.7140", "-72.9360", "3107777777"},
                {"Laura Gómez", "repartidor8@zygo.com", "5.7170", "-72.9330", "3108888888"},
                {"Miguel Díaz", "repartidor9@zygo.com", "5.7130", "-72.9370", "3109999999"},
                {"Sofia Ruiz", "repartidor10@zygo.com", "5.7180", "-72.9320", "3100000001"},
                {"Andrés Castro", "repartidor11@zygo.com", "5.7120", "-72.9380", "3100000002"},
                {"Paula Herrera", "repartidor12@zygo.com", "5.7150", "-72.9350", "3100000003"},
                {"Ricardo Morales", "repartidor13@zygo.com", "5.7145", "-72.9355", "3100000004"},
                {"Valentina Cruz", "repartidor14@zygo.com", "5.7155", "-72.9345", "3100000005"},
                {"Santiago Vargas", "repartidor15@zygo.com", "5.7165", "-72.9355", "3100000006"}
            };
            
            int creados = 0;
            for (int i = 0; i < datosRepartidores.length; i++) {
                String[] datos = datosRepartidores[i];
                
                // Verificar si ya existe
                if (usuarioRepository.existsByEmail(datos[1])) {
                    log.info("   ⏭️ {} ya existe", datos[1]);
                    continue;
                }
                
                Usuario repartidor = new Usuario();
                repartidor.setNombre(datos[0]);
                repartidor.setEmail(datos[1]);
                repartidor.setPassword(passwordEncoder.encode("repartidor123"));
                repartidor.setTelefono(datos[4]);
                repartidor.setDireccion("Base Repartidor " + (i + 1) + " - Sogamoso");
                repartidor.setTipo(Usuario.TipoUsuario.REPARTIDOR);
                repartidor.setLatitud(Double.parseDouble(datos[2]));
                repartidor.setLongitud(Double.parseDouble(datos[3]));
                repartidor.setDisponible(true);
                repartidor.setActivo(true);
                usuarioRepository.save(repartidor);
                creados++;
            }
            
            log.info("✅ {} repartidores creados", creados);
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "Repartidores creados exitosamente",
                "creados", creados,
                "total", usuarioRepository.countByTipo(Usuario.TipoUsuario.REPARTIDOR)
            ));
            
        } catch (Exception e) {
            log.error("❌ Error creando repartidores: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Error creando repartidores: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Endpoint para obtener estadísticas del sistema
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<?> obtenerEstadisticas() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("nodos_totales", graphRepository.count());
        stats.put("restaurantes", graphRepository.countByTipo(Graph.TipoNodo.RESTAURANTE));
        stats.put("intersecciones", graphRepository.countByTipo(Graph.TipoNodo.INTERSECCION));
        stats.put("usuarios_totales", usuarioRepository.count());
        stats.put("repartidores", usuarioRepository.countByTipo(Usuario.TipoUsuario.REPARTIDOR));
        stats.put("clientes", usuarioRepository.countByTipo(Usuario.TipoUsuario.CLIENTE));
        stats.put("admins", usuarioRepository.countByTipo(Usuario.TipoUsuario.ADMIN));
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Endpoint para inicializar TODO de una vez
     */
    @PostMapping("/inicializar-todo")
    public ResponseEntity<?> inicializarTodo() {
        Map<String, Object> resultado = new HashMap<>();
        
        try {
            // Restaurantes
            if (graphRepository.countByTipo(Graph.TipoNodo.RESTAURANTE) < 7) {
                inicializarRestaurantes();
                resultado.put("restaurantes", "✅ Creados");
            } else {
                resultado.put("restaurantes", "⏭️ Ya existen");
            }
            
            // Repartidores
            if (usuarioRepository.countByTipo(Usuario.TipoUsuario.REPARTIDOR) < 15) {
                inicializarRepartidores();
                resultado.put("repartidores", "✅ Creados");
            } else {
                resultado.put("repartidores", "⏭️ Ya existen");
            }
            
            // Admin (si no existe)
            if (usuarioRepository.countByTipo(Usuario.TipoUsuario.ADMIN) == 0) {
                Usuario admin = new Usuario();
                admin.setNombre("Administrador Zygo");
                admin.setEmail("admin@zygo.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setTelefono("3001234567");
                admin.setDireccion("Oficina Central - Sogamoso");
                admin.setTipo(Usuario.TipoUsuario.ADMIN);
                admin.setActivo(true);
                usuarioRepository.save(admin);
                resultado.put("admin", "✅ Creado");
            } else {
                resultado.put("admin", "⏭️ Ya existe");
            }
            
            // Cliente de prueba (si no existe)
            if (!usuarioRepository.existsByEmail("cliente@test.com")) {
                Usuario cliente = new Usuario();
                cliente.setNombre("Carlos Cliente");
                cliente.setEmail("cliente@test.com");
                cliente.setPassword(passwordEncoder.encode("cliente123"));
                cliente.setTelefono("3009876543");
                cliente.setDireccion("Calle 15 #10-20, Sogamoso");
                cliente.setTipo(Usuario.TipoUsuario.CLIENTE);
                cliente.setLatitud(5.7145);
                cliente.setLongitud(-72.9345);
                cliente.setActivo(true);
                usuarioRepository.save(cliente);
                resultado.put("cliente_prueba", "✅ Creado");
            } else {
                resultado.put("cliente_prueba", "⏭️ Ya existe");
            }
            
            resultado.put("mensaje", "Inicialización completada");
            return ResponseEntity.ok(resultado);
            
        } catch (Exception e) {
            log.error("❌ Error en inicialización: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
}