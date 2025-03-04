package com.logistica.agendamiento.config;

import com.logistica.agendamiento.entity.Area;
import com.logistica.agendamiento.entity.TipoServicio;
import com.logistica.agendamiento.entity.Usuario;
import com.logistica.agendamiento.entity.enums.Rol;
import com.logistica.agendamiento.repository.AreaRepository;
import com.logistica.agendamiento.repository.TipoServicioRepository;
import com.logistica.agendamiento.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AreaRepository areaRepository;
    private final TipoServicioRepository tipoServicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Cargar áreas iniciales si no existen
        if (areaRepository.count() == 0) {
            log.info("Inicializando áreas...");

            Area congelados = new Area();
            congelados.setNombre("Congelados");
            congelados.setDescripcion("Área para recepción de productos refrigerados");
            areaRepository.save(congelados);

            Area secos = new Area();
            secos.setNombre("Secos");
            secos.setDescripcion("Área para recepción de productos no refrigerados");
            areaRepository.save(secos);

            log.info("Áreas inicializadas correctamente");
        }

        // Cargar tipos de servicio iniciales si no existen
        if (tipoServicioRepository.count() == 0) {
            log.info("Inicializando tipos de servicio...");

            TipoServicio courier = new TipoServicio();
            courier.setNombre("Courier");
            courier.setDescripcion("Servicio de entrega rápida para paquetes pequeños");
            tipoServicioRepository.save(courier);

            TipoServicio camion = new TipoServicio();
            camion.setNombre("Camión");
            camion.setDescripcion("Transporte medio para entregas regulares");
            tipoServicioRepository.save(camion);

            TipoServicio contenedor = new TipoServicio();
            contenedor.setNombre("Contenedor");
            contenedor.setDescripcion("Transporte grande para entregas de gran volumen");
            tipoServicioRepository.save(contenedor);

            log.info("Tipos de servicio inicializados correctamente");
        }

        // Crear usuario administrador si no existe
        if (usuarioRepository.count() == 0) {
            log.info("Creando usuario administrador...");

            Usuario admin = new Usuario();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123")); // En producción usar una contraseña fuerte
            admin.setEmail("admin@sistema.com");
            admin.setNombre("Administrador");
            admin.setApellido("Sistema");
            admin.setRol(Rol.ADMIN);
            admin.setEstado(true);

            usuarioRepository.save(admin);

            log.info("Usuario administrador creado correctamente");
        }
    }
}