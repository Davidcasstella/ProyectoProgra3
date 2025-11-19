package com.Zygo.proyecto.config;

import com.Zygo.proyecto.model.Edge;
import com.Zygo.proyecto.model.Graph;
import com.Zygo.proyecto.model.Usuario;
import com.Zygo.proyecto.repository.EdgeRepository;
import com.Zygo.proyecto.repository.GraphRepository;
import com.Zygo.proyecto.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataInitializer {
    
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    
    @Autowired
    private GraphRepository graphRepository;
    
    @Autowired
    private EdgeRepository edgeRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Bean
    @Order(1)
    public CommandLineRunner initializeGraphData() {
        return args -> {
            try {
                log.info("========================================");
                log.info("🚀 Sistema Zygo - Inicialización");
                log.info("========================================");
                
                // Verificar si ya existen datos
                if (graphRepository.count() > 0) {
                    log.info("✅ Base de datos ya contiene datos");
                    mostrarEstadisticas();
                    return;
                }
                
                // 1. Crear grafo básico (intersecciones)
                log.info("📍 Creando grafo de calles...");
                List<Graph> intersecciones = crearIntersecciones();
                log.info("✅ {} intersecciones creadas", intersecciones.size());
                
                // 2. Crear restaurantes
                log.info("🍽️ Creando 7 restaurantes...");
                List<Graph> restaurantes = crearRestaurantes();
                log.info("✅ {} restaurantes creados", restaurantes.size());
                
                // 3. Crear aristas (calles)
                log.info("🛣️ Creando calles (aristas)...");
                List<Edge> calles = crearCalles(restaurantes, intersecciones);
                log.info("✅ {} aristas creadas", calles.size());
                
                // 4. Crear usuarios
                log.info("👥 Creando usuarios del sistema...");
                crearUsuarios();
                log.info("✅ Usuarios creados");
                
                log.info("========================================");
                log.info("🎉 Inicialización completada");
                mostrarEstadisticas();
                mostrarCredenciales();
                
            } catch (Exception e) {
                log.error("❌ Error en inicialización: {}", e.getMessage(), e);
            }
        };
    }
    
    /**
     * Crea intersecciones (nodos del grafo) para formar la red de calles
     */
    private List<Graph> crearIntersecciones() {
        List<Graph> intersecciones = new ArrayList<>();
        
        String[][] datosIntersecciones = {
            {"Intersección Norte", "5.7190", "-72.9310"},
            {"Intersección Sur", "5.7110", "-72.9390"},
            {"Intersección Este", "5.7150", "-72.9300"},
            {"Intersección Oeste", "5.7150", "-72.9400"},
            {"Intersección Centro", "5.7150", "-72.9350"},
            {"Intersección NE", "5.7180", "-72.9320"},
            {"Intersección NO", "5.7180", "-72.9380"},
            {"Intersección SE", "5.7120", "-72.9320"},
            {"Intersección SO", "5.7120", "-72.9380"},
            {"Intersección Plaza", "5.7160", "-72.9340"},
            {"Intersección Terminal", "5.7140", "-72.9360"},
            {"Intersección Parque", "5.7170", "-72.9330"}
        };
        
        for (String[] datos : datosIntersecciones) {
            Graph interseccion = new Graph();
            interseccion.setNombre(datos[0]);
            interseccion.setLatitud(Double.parseDouble(datos[1]));
            interseccion.setLongitud(Double.parseDouble(datos[2]));
            interseccion.setTipo(Graph.TipoNodo.INTERSECCION);
            interseccion.setDireccionCompleta(datos[0]);
            interseccion.setActivo(true);
            intersecciones.add(graphRepository.save(interseccion));
        }
        
        return intersecciones;
    }
    
    /**
     * Crea 7 restaurantes distribuidos por Sogamoso
     */
    private List<Graph> crearRestaurantes() {
        List<Graph> restaurantes = new ArrayList<>();
        
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
            restaurantes.add(graphRepository.save(restaurante));
        }
        
        return restaurantes;
    }
    
    /**
     * Crea aristas (calles) conectando los nodos
     */
    private List<Edge> crearCalles(List<Graph> restaurantes, List<Graph> intersecciones) {
        List<Edge> calles = new ArrayList<>();
        
        // Conectar restaurantes con intersecciones cercanas
        for (Graph restaurante : restaurantes) {
            intersecciones.stream()
                .sorted((i1, i2) -> Double.compare(
                    calcularDistancia(restaurante, i1),
                    calcularDistancia(restaurante, i2)
                ))
                .limit(3)
                .forEach(interseccion -> {
                    double distancia = calcularDistancia(restaurante, interseccion);
                    calles.add(crearArista(restaurante, interseccion, distancia, "Calle Principal"));
                });
        }
        
        // Conectar intersecciones entre sí
        for (int i = 0; i < intersecciones.size(); i++) {
            for (int j = i + 1; j < intersecciones.size(); j++) {
                Graph nodo1 = intersecciones.get(i);
                Graph nodo2 = intersecciones.get(j);
                double distancia = calcularDistancia(nodo1, nodo2);
                
                if (distancia < 2.0) {
                    calles.add(crearArista(nodo1, nodo2, distancia, "Avenida"));
                }
            }
        }
        
        return calles;
    }
    
    private Edge crearArista(Graph origen, Graph destino, double distanciaKm, String nombreCalle) {
        Edge arista = new Edge();
        arista.setNodoOrigen(origen);
        arista.setNodoDestino(destino);
        arista.setDistanciaKm(distanciaKm);
        arista.setTiempoEstimadoMinutos((int)(distanciaKm * 5));
        arista.setNombreCalle(nombreCalle);
        arista.setEsBidireccional(true);
        arista.setFactorTrafico(1.0);
        arista.setTipoCalle(Edge.TipoCalle.AVENIDA_PRINCIPAL);
        arista.setActivo(true);
        return edgeRepository.save(arista);
    }
    
    /**
     * Crea usuarios: 1 admin, 1 cliente, 15 repartidores
     */
    private void crearUsuarios() {
        // 1. ADMIN
        Usuario admin = new Usuario();
        admin.setNombre("Administrador Zygo");
        admin.setEmail("admin@zygo.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setTelefono("3001234567");
        admin.setDireccion("Oficina Central - Sogamoso");
        admin.setTipo(Usuario.TipoUsuario.ADMIN);
        admin.setActivo(true);
        usuarioRepository.save(admin);
        
        // 2. CLIENTE DE PRUEBA
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
        
        // 3. 15 REPARTIDORES
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
        
        for (int i = 0; i < datosRepartidores.length; i++) {
            String[] datos = datosRepartidores[i];
            Usuario repartidor = new Usuario();
            repartidor.setNombre(datos[0]);
            repartidor.setEmail(datos[1]);
            repartidor.setPassword(passwordEncoder.encode("repartidor123"));
            repartidor.setTelefono(datos[4]);
            repartidor.setDireccion("Base " + (i + 1) + " - Sogamoso");
            repartidor.setTipo(Usuario.TipoUsuario.REPARTIDOR);
            repartidor.setLatitud(Double.parseDouble(datos[2]));
            repartidor.setLongitud(Double.parseDouble(datos[3]));
            repartidor.setDisponible(true);
            repartidor.setActivo(true);
            usuarioRepository.save(repartidor);
        }
    }
    
    private double calcularDistancia(Graph nodo1, Graph nodo2) {
        double deltaLat = nodo1.getLatitud() - nodo2.getLatitud();
        double deltaLon = nodo1.getLongitud() - nodo2.getLongitud();
        return Math.sqrt(deltaLat * deltaLat + deltaLon * deltaLon) * 111;
    }
    
    private void mostrarEstadisticas() {
        log.info("========================================");
        log.info("📊 Estadísticas del Sistema:");
        log.info("   - Nodos en el grafo: {}", graphRepository.count());
        log.info("   - Restaurantes: {}", graphRepository.countByTipo(Graph.TipoNodo.RESTAURANTE));
        log.info("   - Intersecciones: {}", graphRepository.countByTipo(Graph.TipoNodo.INTERSECCION));
        log.info("   - Aristas (calles): {}", edgeRepository.count());
        log.info("   - Usuarios totales: {}", usuarioRepository.count());
        log.info("   - Repartidores: {}", usuarioRepository.countByTipo(Usuario.TipoUsuario.REPARTIDOR));
        log.info("   - Clientes: {}", usuarioRepository.countByTipo(Usuario.TipoUsuario.CLIENTE));
        log.info("========================================");
    }
    
    private void mostrarCredenciales() {
        log.info("🔑 Credenciales de acceso:");
        log.info("   Admin:      admin@zygo.com / admin123");
        log.info("   Cliente:    cliente@test.com / cliente123");
        log.info("   Repartidor: repartidor1@zygo.com / repartidor123");
        log.info("========================================");
    }
}