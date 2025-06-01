package com.logistica.agendamiento.service.impl;

import com.logistica.agendamiento.dto.HorarioProveedorDTO;
import com.logistica.agendamiento.dto.PlantillaHorarioDTO;
import com.logistica.agendamiento.entity.*;
import com.logistica.agendamiento.entity.enums.DiaSemana;
import com.logistica.agendamiento.entity.enums.EstadoAnden;
import com.logistica.agendamiento.entity.enums.EstadoReserva;
import com.logistica.agendamiento.exception.BadRequestException;
import com.logistica.agendamiento.exception.ResourceNotFoundException;
import com.logistica.agendamiento.repository.*;
import com.logistica.agendamiento.service.ExcelProcessingService;
import com.logistica.agendamiento.service.PlantillaHorarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlantillaHorarioServiceImpl implements PlantillaHorarioService {

    private final PlantillaHorarioRepository plantillaHorarioRepository;
    private final ProveedorRepository proveedorRepository;
    private final UsuarioRepository usuarioRepository;
    private final AreaRepository areaRepository;
    private final AndenRepository andenRepository;
    private final TipoServicioRepository tipoServicioRepository;
    private final ReservaRepository reservaRepository;
    private final TransporteRepository transporteRepository;
    private final ExcelProcessingService excelProcessingService;


    @Override
    @Transactional
    public List<PlantillaHorario> cargarDesdeExcel(MultipartFile archivo) {
        log.info("Iniciando carga de plantilla desde Excel: {}", archivo.getOriginalFilename());

        // Procesar el Excel
        List<PlantillaHorarioDTO> plantillasDTO = excelProcessingService.procesarExcelPlantilla(archivo);

        if (plantillasDTO.isEmpty()) {
            throw new BadRequestException("El archivo Excel no contiene datos válidos");
        }

        // ✅ CAMBIO: Eliminar físicamente las plantillas anteriores
        List<PlantillaHorario> plantillasAnteriores = plantillaHorarioRepository.findByActivoTrue();
        if (!plantillasAnteriores.isEmpty()) {
            log.info("Eliminando {} plantillas anteriores físicamente", plantillasAnteriores.size());
            plantillaHorarioRepository.deleteAll(plantillasAnteriores); // ← ELIMINACIÓN FÍSICA
        }

        // ✅ Crear nuevas plantillas SIN asignación automática
        List<PlantillaHorario> nuevasPlantillas = new ArrayList<>();
        for (PlantillaHorarioDTO dto : plantillasDTO) {
            PlantillaHorario plantilla = convertirDeDTO(dto);

            // ✅ ASEGURAR que NO tenga recursos asignados
            plantilla.setArea(null);
            plantilla.setAnden(null);
            plantilla.setTipoServicio(null);

            log.debug("Creando plantilla para {} en {}: area={}, anden={}, tipoServicio={}",
                    plantilla.getProveedor().getNombre(),
                    plantilla.getDia(),
                    plantilla.getArea(),
                    plantilla.getAnden(),
                    plantilla.getTipoServicio());

            nuevasPlantillas.add(plantilla);
        }

        List<PlantillaHorario> plantillasGuardadas = plantillaHorarioRepository.saveAll(nuevasPlantillas);

        log.info("Cargadas {} plantillas de horarios desde Excel (SIN recursos automáticos)", plantillasGuardadas.size());
        return plantillasGuardadas;
    }

    @Override
    public PlantillaHorario obtenerHorarioProveedor(Long proveedorId, DiaSemana dia) {
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + proveedorId));

        return plantillaHorarioRepository.findByProveedorAndDiaAndActivoTrue(proveedor, dia)
                .orElse(null);
    }

    @Override
    public List<PlantillaHorario> obtenerHorariosSemana(Long proveedorId) {
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + proveedorId));

        return plantillaHorarioRepository.findByProveedorAndActivoTrue(proveedor);
    }

    @Override
    @Transactional
    public PlantillaHorario crearHorario(PlantillaHorarioDTO dto) {
        Proveedor proveedor = proveedorRepository.findById(dto.getProveedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));

        if (plantillaHorarioRepository.existsByProveedorAndDiaAndActivoTrue(proveedor, dto.getDia())) {
            throw new BadRequestException("Ya existe un horario activo para este proveedor en " + dto.getDia());
        }

        PlantillaHorario plantilla = convertirDeDTO(dto);

        // ✅ CAMBIO: NO asignar recursos automáticamente
        // ❌ asignarRecursosAutomaticamente(plantilla);
        // Los campos area, anden, tipoServicio quedan NULL para que el proveedor los seleccione

        return plantillaHorarioRepository.save(plantilla);
    }

    @Override
    @Transactional
    public PlantillaHorario actualizarHorario(Long id, PlantillaHorarioDTO dto) {
        PlantillaHorario plantilla = plantillaHorarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla de horario no encontrada con ID: " + id));

        plantilla.setDia(dto.getDia());
        plantilla.setNumeroPersonas(dto.getNumeroPersonas());
        plantilla.setHoraInicio(dto.getHoraInicio());
        plantilla.setHoraFin(dto.getHoraFin());
        plantilla.setTiempoDescarga(dto.getTiempoDescarga());

        // ✅ CAMBIO: Solo asignar recursos si vienen especificados en el DTO
        if (dto.getAreaId() != null) {
            Area area = areaRepository.findById(dto.getAreaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Área no encontrada"));
            plantilla.setArea(area);
        }

        if (dto.getAndenId() != null) {
            Anden anden = andenRepository.findById(dto.getAndenId())
                    .orElseThrow(() -> new ResourceNotFoundException("Andén no encontrado"));
            plantilla.setAnden(anden);
        }

        if (dto.getTipoServicioId() != null) {
            TipoServicio tipoServicio = tipoServicioRepository.findById(dto.getTipoServicioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de servicio no encontrado"));
            plantilla.setTipoServicio(tipoServicio);
        }

        return plantillaHorarioRepository.save(plantilla);
    }

    @Override
    @Transactional
    public void eliminarHorario(Long id) {
        PlantillaHorario plantilla = plantillaHorarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla de horario no encontrada con ID: " + id));

        // ✅ CAMBIO: Eliminar físicamente en lugar de soft delete
        plantillaHorarioRepository.delete(plantilla);

        log.info("Plantilla eliminada físicamente: {}", id);
    }

    @Override
    @Transactional
    public void eliminarHorariosMultiple(List<Long> ids) {
        List<PlantillaHorario> plantillas = plantillaHorarioRepository.findAllById(ids);

        if (plantillas.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron plantillas con los IDs proporcionados");
        }

        // ✅ Eliminar físicamente todas las plantillas
        plantillaHorarioRepository.deleteAll(plantillas);

        log.info("Eliminadas {} plantillas físicamente", plantillas.size());
    }

    @Override
    public List<PlantillaHorario> obtenerPlantillaPorDia(DiaSemana dia) {
        return plantillaHorarioRepository.findByDiaAndActivoTrue(dia);
    }

    @Override
    public List<PlantillaHorario> obtenerTodasLasPlantillas() {
        return plantillaHorarioRepository.findByActivoTrue();
    }

    @Override
    public HorarioProveedorDTO obtenerHorarioProveedorPorFecha(String username, LocalDate fecha) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));

        Proveedor proveedor = proveedorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado para el usuario: " + username));

        DiaSemana dia = convertirDayOfWeekADiaSemana(fecha.getDayOfWeek());

        Optional<PlantillaHorario> plantillaOpt = plantillaHorarioRepository
                .findByProveedorAndDiaAndActivoTrue(proveedor, dia);

        if (plantillaOpt.isEmpty()) {
            throw new ResourceNotFoundException("No hay horario asignado para " + dia + " al proveedor " + proveedor.getNombre());
        }

        PlantillaHorario plantilla = plantillaOpt.get();

        // Verificar si ya tiene reserva para esa fecha
        Optional<Reserva> reservaOpt = reservaRepository.findByProveedorAndFechaAndEstado(
                proveedor, fecha, EstadoReserva.PENDIENTE_CONFIRMACION);

        boolean tieneReserva = reservaOpt.isPresent();
        Long reservaId = tieneReserva ? reservaOpt.get().getId() : null;
        String estadoReserva = tieneReserva ? reservaOpt.get().getEstado().name() : null;

        // Verificar si puede confirmar (solo si está en PENDIENTE_CONFIRMACION)
        boolean puedeConfirmar = tieneReserva &&
                reservaOpt.get().getEstado() == EstadoReserva.PENDIENTE_CONFIRMACION;

        HorarioProveedorDTO horarioDTO = new HorarioProveedorDTO();
        horarioDTO.setFecha(fecha);
        horarioDTO.setDia(dia);
        horarioDTO.setHoraInicio(plantilla.getHoraInicio());
        horarioDTO.setHoraFin(plantilla.getHoraFin());
        horarioDTO.setTiempoDescarga(plantilla.getTiempoDescarga());
        horarioDTO.setNumeroPersonas(plantilla.getNumeroPersonas());
        horarioDTO.setAreaNombre(plantilla.getArea() != null ? plantilla.getArea().getNombre() : "No asignada");
        horarioDTO.setAndenNumero(plantilla.getAnden() != null ? plantilla.getAnden().getNumero() : null);
        horarioDTO.setTipoServicioNombre(plantilla.getTipoServicio() != null ? plantilla.getTipoServicio().getNombre() : "No asignado");
        horarioDTO.setTieneReserva(tieneReserva);
        horarioDTO.setReservaId(reservaId);
        horarioDTO.setEstadoReserva(estadoReserva);
        horarioDTO.setPuedeConfirmar(puedeConfirmar);

        return horarioDTO;
    }

    @Override
    public List<HorarioProveedorDTO> obtenerHorarioProveedorSemana(String username, LocalDate fechaInicio) {
        return fechaInicio.datesUntil(fechaInicio.plusDays(7))
                .map(fecha -> {
                    try {
                        return obtenerHorarioProveedorPorFecha(username, fecha);
                    } catch (ResourceNotFoundException e) {
                        HorarioProveedorDTO horarioVacio = new HorarioProveedorDTO();
                        horarioVacio.setFecha(fecha);
                        horarioVacio.setDia(convertirDayOfWeekADiaSemana(fecha.getDayOfWeek()));
                        horarioVacio.setTieneReserva(false);
                        horarioVacio.setPuedeConfirmar(false);
                        return horarioVacio;
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void generarReservasDesdeePlantillas(LocalDate fecha) {
        DiaSemana dia = convertirDayOfWeekADiaSemana(fecha.getDayOfWeek());
        List<PlantillaHorario> plantillasDelDia = plantillaHorarioRepository.findByDiaAndActivoTrue(dia);

        log.info("🔄 Iniciando generación de PRE-RESERVAS para {} ({})", fecha, dia);
        log.info("📋 Plantillas encontradas: {}", plantillasDelDia.size());

        if (plantillasDelDia.isEmpty()) {
            log.warn("⚠️ No hay plantillas activas para el día {}", dia);
            return;
        }

        int exitosas = 0;
        int fallidas = 0;
        int yaExistentes = 0;

        for (PlantillaHorario plantilla : plantillasDelDia) {
            try {
                log.debug("🔍 Procesando plantilla ID {} - Proveedor: {}",
                        plantilla.getId(),
                        plantilla.getProveedor().getNombre());

                // Verificar si ya existe reserva para este proveedor y fecha
                List<Reserva> reservasExistentes = reservaRepository
                        .findByProveedorAndFecha(plantilla.getProveedor(), fecha);

                if (!reservasExistentes.isEmpty()) {
                    yaExistentes++;
                    log.debug("⏭️ Ya existe reserva para proveedor {} en fecha {} - Estados: {}",
                            plantilla.getProveedor().getNombre(),
                            fecha,
                            reservasExistentes.stream()
                                    .map(r -> r.getEstado().name())
                                    .collect(Collectors.joining(", ")));
                    continue;
                }

                // Crear PRE-RESERVA
                log.debug("✨ Creando PRE-RESERVA para proveedor {}",
                        plantilla.getProveedor().getNombre());

                crearReservaDesdePlugantilla(plantilla, fecha);
                exitosas++;

                log.info("✅ PRE-RESERVA creada - Proveedor: {}, Fecha: {}, Horario: {} - {}",
                        plantilla.getProveedor().getNombre(),
                        fecha,
                        plantilla.getHoraInicio(),
                        plantilla.getHoraFin());

            } catch (Exception e) {
                fallidas++;
                log.error("❌ Error procesando plantilla ID {} (Proveedor: {}) para fecha {}: {}",
                        plantilla.getId(),
                        plantilla.getProveedor().getNombre(),
                        fecha,
                        e.getMessage(), e);
            }
        }

        log.info("📊 Generación de PRE-RESERVAS para {}: {} exitosas, {} ya existentes, {} fallidas de {} plantillas",
                fecha, exitosas, yaExistentes, fallidas, plantillasDelDia.size());

        if (exitosas == 0 && fallidas == 0 && yaExistentes > 0) {
            log.info("ℹ️ Todas las reservas para {} ya existían previamente", fecha);
        } else if (exitosas == 0 && fallidas == 0 && yaExistentes == 0) {
            log.warn("⚠️ No se generaron reservas y no había existentes. Revisar plantillas.");
        }
    }
    @Override
    @Transactional
    public void generarReservasSemanaCompleta(LocalDate fechaInicio) {
        for (int i = 0; i < 7; i++) {
            LocalDate fecha = fechaInicio.plusDays(i);
            generarReservasDesdeePlantillas(fecha);
        }
    }

    @Override
    public List<PlantillaHorarioDTO> convertirADTOList(List<PlantillaHorario> plantillas) {
        return plantillas.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    public PlantillaHorarioDTO convertirADTO(PlantillaHorario plantilla) {
        PlantillaHorarioDTO dto = new PlantillaHorarioDTO();
        dto.setId(plantilla.getId());
        dto.setDia(plantilla.getDia());
        dto.setProveedorId(plantilla.getProveedor().getId());
        dto.setProveedorNombre(plantilla.getProveedor().getNombre());
        dto.setNumeroPersonas(plantilla.getNumeroPersonas());
        dto.setHoraInicio(plantilla.getHoraInicio());
        dto.setHoraFin(plantilla.getHoraFin());
        dto.setTiempoDescarga(plantilla.getTiempoDescarga());
        dto.setActivo(plantilla.getActivo());

        if (plantilla.getArea() != null) {
            dto.setAreaId(plantilla.getArea().getId());
            dto.setAreaNombre(plantilla.getArea().getNombre());
        }

        if (plantilla.getAnden() != null) {
            dto.setAndenId(plantilla.getAnden().getId());
            dto.setAndenNumero(plantilla.getAnden().getNumero());
        }

        if (plantilla.getTipoServicio() != null) {
            dto.setTipoServicioId(plantilla.getTipoServicio().getId());
            dto.setTipoServicioNombre(plantilla.getTipoServicio().getNombre());
        }

        return dto;
    }

    // MÉTODOS PRIVADOS

    private PlantillaHorario convertirDeDTO(PlantillaHorarioDTO dto) {
        PlantillaHorario plantilla = new PlantillaHorario();

        Proveedor proveedor = proveedorRepository.findById(dto.getProveedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + dto.getProveedorId()));

        plantilla.setProveedor(proveedor);
        plantilla.setDia(dto.getDia());
        plantilla.setNumeroPersonas(dto.getNumeroPersonas());
        plantilla.setHoraInicio(dto.getHoraInicio());
        plantilla.setHoraFin(dto.getHoraFin());
        plantilla.setTiempoDescarga(dto.getTiempoDescarga());
        plantilla.setActivo(true);

        return plantilla;
    }

    private void asignarRecursosAutomaticamente(PlantillaHorario plantilla) {
        // Asignar área por defecto (primera disponible)
        if (plantilla.getArea() == null) {
            List<Area> areas = areaRepository.findAll();
            if (!areas.isEmpty()) {
                plantilla.setArea(areas.get(0)); // Por ahora asignar la primera
            }
        }

        // Asignar tipo de servicio basado en número de personas
        if (plantilla.getTipoServicio() == null) {
            TipoServicio tipoServicio = determinarTipoServicioPorPersonas(plantilla.getNumeroPersonas());
            plantilla.setTipoServicio(tipoServicio);
        }

        // Asignar andén disponible
        if (plantilla.getAnden() == null && plantilla.getArea() != null) {
            Anden anden = buscarAndenDisponible(plantilla.getArea(), plantilla.getTipoServicio());
            plantilla.setAnden(anden);
        }
    }

    private TipoServicio determinarTipoServicioPorPersonas(Integer numeroPersonas) {
        // Lógica simple: 1 persona = Courier, 2-3 = Camión, 4+ = Contenedor
        String tipoNombre;
        if (numeroPersonas == 1) {
            tipoNombre = "Courier";
        } else if (numeroPersonas <= 3) {
            tipoNombre = "Camión";
        } else {
            tipoNombre = "Contenedor";
        }

        return tipoServicioRepository.findByNombre(tipoNombre)
                .orElse(tipoServicioRepository.findAll().get(0)); // Fallback al primero
    }

    private Anden buscarAndenDisponible(Area area, TipoServicio tipoServicio) {
        // Buscar andén compatible con el tipo de servicio
        boolean necesitaContenedor = tipoServicio != null &&
                tipoServicio.getNombre().equalsIgnoreCase("Contenedor");

        List<Anden> andenesArea = andenRepository.findByArea(area);

        return andenesArea.stream()
                .filter(anden -> necesitaContenedor == anden.getExclusivoContenedor())
                .findFirst()
                .orElse(andenesArea.isEmpty() ? null : andenesArea.get(0));
    }

    // ✅ MÉTODO AUXILIAR MEJORADO TAMBIÉN
    private void crearReservaDesdePlugantilla(PlantillaHorario plantilla, LocalDate fecha) {
        try {
            log.debug("🏗️ Iniciando creación de PRE-RESERVA...");

            Reserva reserva = new Reserva();

            // Validar que el proveedor existe y está activo
            if (plantilla.getProveedor() == null) {
                throw new IllegalStateException("La plantilla no tiene proveedor asignado");
            }

            if (!plantilla.getProveedor().getEstado()) {
                log.warn("⚠️ Proveedor {} está inactivo, saltando creación de reserva",
                        plantilla.getProveedor().getNombre());
                return;
            }

            // ✅ SOLO datos básicos de la plantilla (campos obligatorios)
            reserva.setProveedor(plantilla.getProveedor());
            reserva.setFecha(fecha);
            reserva.setHoraInicio(plantilla.getHoraInicio());
            reserva.setHoraFin(plantilla.getHoraFin());
            reserva.setEstado(EstadoReserva.PENDIENTE_CONFIRMACION);
            reserva.setDescripcion("PRE-RESERVA: Proveedor debe completar datos de área, andén, tipo de servicio y transporte");

            // ✅ CRÍTICO: Los campos area, anden, tipoServicio, transporte quedan NULL
            reserva.setArea(null);
            reserva.setAnden(null);
            reserva.setTipoServicio(null);
            reserva.setTransporte(null);

            log.debug("💾 Guardando PRE-RESERVA en base de datos...");
            Reserva reservaGuardada = reservaRepository.save(reserva);

            log.info("✅ PRE-RESERVA creada exitosamente - ID: {}, Proveedor: {}, Fecha: {}, Estado: {}",
                    reservaGuardada.getId(),
                    plantilla.getProveedor().getNombre(),
                    fecha,
                    reservaGuardada.getEstado());

        } catch (Exception e) {
            log.error("💥 Error detallado creando PRE-RESERVA para proveedor {} en fecha {}",
                    plantilla.getProveedor().getNombre(),
                    fecha, e);

            // Re-lanzar la excepción para que el método padre la maneje
            throw new RuntimeException("Error al crear PRE-RESERVA: " + e.getMessage(), e);
        }
    }


    private DiaSemana convertirDayOfWeekADiaSemana(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> DiaSemana.LUNES;
            case TUESDAY -> DiaSemana.MARTES;
            case WEDNESDAY -> DiaSemana.MIERCOLES;
            case THURSDAY -> DiaSemana.JUEVES;
            case FRIDAY -> DiaSemana.VIERNES;
            case SATURDAY -> DiaSemana.SABADO;
            case SUNDAY -> DiaSemana.DOMINGO;
        };
    }


}
