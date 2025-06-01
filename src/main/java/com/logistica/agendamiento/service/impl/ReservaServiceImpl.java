package com.logistica.agendamiento.service.impl;

import com.logistica.agendamiento.dto.*;
import com.logistica.agendamiento.entity.*;
import com.logistica.agendamiento.entity.enums.EstadoAnden;
import com.logistica.agendamiento.entity.enums.EstadoReserva;
import com.logistica.agendamiento.exception.BadRequestException;
import com.logistica.agendamiento.exception.ResourceNotFoundException;
import com.logistica.agendamiento.repository.*;
import com.logistica.agendamiento.service.EmailService;
import com.logistica.agendamiento.service.ReservaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;
    private final ProveedorRepository proveedorRepository;
    private final AreaRepository areaRepository;
    private final AndenRepository andenRepository;
    private final TipoServicioRepository tipoServicioRepository;
    private final TransporteRepository transporteRepository;
    private final TransportistaRepository transportistaRepository;
    private final EmailService emailService;
    private final UsuarioRepository usuarioRepository;

    @Override
    public List<ReservaDTO> obtenerTodasLasReservas() {
        return reservaRepository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservaDTO> obtenerReservasPorFecha(LocalDate fecha) {
        return reservaRepository.findByFecha(fecha).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservaDTO> obtenerReservasPorProveedorId(Long proveedorId) {
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + proveedorId));

        return reservaRepository.findByProveedor(proveedor).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservaDTO> obtenerReservasPorAreaId(Long areaId) {
        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("Área no encontrada con ID: " + areaId));

        return reservaRepository.findByArea(area).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservaDTO> obtenerReservasPorAndenId(Long andenId) {
        Anden anden = andenRepository.findById(andenId)
                .orElseThrow(() -> new ResourceNotFoundException("Andén no encontrado con ID: " + andenId));

        return reservaRepository.findByAnden(anden).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservaDTO> obtenerReservasPorEstado(EstadoReserva estado) {
        return reservaRepository.findByEstado(estado).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    public ReservaDetalleDTO obtenerReservaPorId(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con ID: " + id));
        return convertirADetalleDTO(reserva);
    }

    @Override
    @Transactional
    public ReservaDetalleDTO crearReserva(ReservaDTO reservaDTO) {
        // Validar y obtener entidades relacionadas
        Proveedor proveedor = proveedorRepository.findById(reservaDTO.getProveedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + reservaDTO.getProveedorId()));

        Area area = areaRepository.findById(reservaDTO.getAreaId())
                .orElseThrow(() -> new ResourceNotFoundException("Área no encontrada con ID: " + reservaDTO.getAreaId()));

        Anden anden = andenRepository.findById(reservaDTO.getAndenId())
                .orElseThrow(() -> new ResourceNotFoundException("Andén no encontrado con ID: " + reservaDTO.getAndenId()));

        TipoServicio tipoServicio = tipoServicioRepository.findById(reservaDTO.getTipoServicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de servicio no encontrado con ID: " + reservaDTO.getTipoServicioId()));

        // Validar si el andén pertenece al área seleccionada
        if (!anden.getArea().getId().equals(area.getId())) {
            throw new BadRequestException("El andén seleccionado no pertenece al área especificada");
        }

        // Crear y guardar el transporte
        Transporte transporte = new Transporte();
        transporte.setTipo(reservaDTO.getTransporteTipo());
        transporte.setMarca(reservaDTO.getTransporteMarca());
        transporte.setModelo(reservaDTO.getTransporteModelo());
        transporte.setPlaca(reservaDTO.getTransportePlaca());
        transporte.setCapacidad(reservaDTO.getTransporteCapacidad());

        Transporte transporteSaved = transporteRepository.save(transporte);

        // Crear y guardar transportistas
        List<Transportista> transportistas = new ArrayList<>();

        // Conductor
        Transportista conductor = new Transportista();
        conductor.setTransporte(transporteSaved);
        conductor.setNombres(reservaDTO.getConductorNombres());
        conductor.setApellidos(reservaDTO.getConductorApellidos());
        conductor.setCedula(reservaDTO.getConductorCedula());
        conductor.setEsConductor(true);
        transportistas.add(conductor);

        // Ayudantes (si existen)
        if (reservaDTO.getAyudantes() != null && !reservaDTO.getAyudantes().isEmpty()) {
            for (AyudanteDTO ayudanteDTO : reservaDTO.getAyudantes()) {
                Transportista ayudante = new Transportista();
                ayudante.setTransporte(transporteSaved);
                ayudante.setNombres(ayudanteDTO.getNombres());
                ayudante.setApellidos(ayudanteDTO.getApellidos());
                ayudante.setCedula(ayudanteDTO.getCedula());
                ayudante.setEsConductor(false);
                transportistas.add(ayudante);
            }
        }

        transportistaRepository.saveAll(transportistas);

        // Crear y guardar la reserva
        Reserva reserva = new Reserva();
        reserva.setProveedor(proveedor);
        reserva.setArea(area);
        reserva.setAnden(anden);
        reserva.setTipoServicio(tipoServicio);
        reserva.setTransporte(transporteSaved);
        reserva.setFecha(reservaDTO.getFecha());
        reserva.setHoraInicio(reservaDTO.getHoraInicio());
        reserva.setHoraFin(reservaDTO.getHoraFin());
        reserva.setEstado(EstadoReserva.PENDIENTE);
        reserva.setDescripcion(reservaDTO.getDescripcion());

        Reserva reservaSaved = reservaRepository.save(reserva);

        ReservaDetalleDTO reservaDetalle = convertirADetalleDTO(reservaSaved);
        emailService.enviarConfirmacionReserva(reservaDetalle, reserva.getProveedor().getEmail());

        return reservaDetalle;
    }

    @Override
    @Transactional
    public ReservaDetalleDTO actualizarReserva(Long id, ReservaDTO reservaDTO) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con ID: " + id));

        // Solo se permite actualizar reservas en estado PENDIENTE
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new BadRequestException("Solo se pueden modificar reservas en estado PENDIENTE");
        }

        // Validar y obtener entidades relacionadas
        Area area = areaRepository.findById(reservaDTO.getAreaId())
                .orElseThrow(() -> new ResourceNotFoundException("Área no encontrada con ID: " + reservaDTO.getAreaId()));

        Anden anden = andenRepository.findById(reservaDTO.getAndenId())
                .orElseThrow(() -> new ResourceNotFoundException("Andén no encontrado con ID: " + reservaDTO.getAndenId()));

        TipoServicio tipoServicio = tipoServicioRepository.findById(reservaDTO.getTipoServicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de servicio no encontrado con ID: " + reservaDTO.getTipoServicioId()));

        // Validar si el andén pertenece al área seleccionada
        if (!anden.getArea().getId().equals(area.getId())) {
            throw new BadRequestException("El andén seleccionado no pertenece al área especificada");
        }

        // Validar si el andén es adecuado para el tipo de servicio
        boolean esContenedor = tipoServicio.getNombre().equalsIgnoreCase("Contenedor");
        if (esContenedor && !anden.getExclusivoContenedor() || !esContenedor && anden.getExclusivoContenedor()) {
            throw new BadRequestException("El andén seleccionado no es compatible con el tipo de servicio");
        }


        // Actualizar datos de transporte
        Transporte transporte = reserva.getTransporte();
        transporte.setTipo(reservaDTO.getTransporteTipo());
        transporte.setMarca(reservaDTO.getTransporteMarca());
        transporte.setModelo(reservaDTO.getTransporteModelo());
        transporte.setPlaca(reservaDTO.getTransportePlaca());
        transporte.setCapacidad(reservaDTO.getTransporteCapacidad());

        transporteRepository.save(transporte);

        // Actualizar transportistas
        // Primero, eliminar todos los transportistas actuales
        transportistaRepository.deleteAll(transporte.getTransportistas());

        List<Transportista> transportistas = new ArrayList<>();

        // Conductor
        Transportista conductor = new Transportista();
        conductor.setTransporte(transporte);
        conductor.setNombres(reservaDTO.getConductorNombres());
        conductor.setApellidos(reservaDTO.getConductorApellidos());
        conductor.setCedula(reservaDTO.getConductorCedula());
        conductor.setEsConductor(true);
        transportistas.add(conductor);

        // Ayudantes (si existen)
        if (reservaDTO.getAyudantes() != null && !reservaDTO.getAyudantes().isEmpty()) {
            for (AyudanteDTO ayudanteDTO : reservaDTO.getAyudantes()) {
                Transportista ayudante = new Transportista();
                ayudante.setTransporte(transporte);
                ayudante.setNombres(ayudanteDTO.getNombres());
                ayudante.setApellidos(ayudanteDTO.getApellidos());
                ayudante.setCedula(ayudanteDTO.getCedula());
                ayudante.setEsConductor(false);
                transportistas.add(ayudante);
            }
        }

        transportistaRepository.saveAll(transportistas);

        // Actualizar la reserva
        reserva.setArea(area);
        reserva.setAnden(anden);
        reserva.setTipoServicio(tipoServicio);
        reserva.setFecha(reservaDTO.getFecha());
        reserva.setHoraInicio(reservaDTO.getHoraInicio());
        reserva.setHoraFin(reservaDTO.getHoraFin());
        reserva.setDescripcion(reservaDTO.getDescripcion());

        Reserva reservaActualizada = reservaRepository.save(reserva);

        // TODO: Actualizar documentos si es necesario

        // TODO: Enviar notificación por correo sobre la actualización

        return convertirADetalleDTO(reservaActualizada);
    }

    @Override
    @Transactional
    public ReservaDetalleDTO actualizarEstadoReserva(Long id, EstadoReserva estado) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con ID: " + id));

        // Validar la transición de estado
        validarTransicionEstado(reserva.getEstado(), estado);

        reserva.setEstado(estado);
        Reserva reservaActualizada = reservaRepository.save(reserva);

        // Si el estado es EN_RECEPCION, actualizar el estado del andén a OCUPADO
        if (estado == EstadoReserva.EN_RECEPCION) {
            Anden anden = reserva.getAnden();
            anden.setEstado(EstadoAnden.OCUPADO);
            andenRepository.save(anden);
        }

        // Si el estado es COMPLETADA, actualizar el estado del andén a DISPONIBLE
        if (estado == EstadoReserva.COMPLETADA) {
            Anden anden = reserva.getAnden();
            anden.setEstado(EstadoAnden.DISPONIBLE);
            andenRepository.save(anden);
        }

        // Enviar notificación por correo sobre el cambio de estado
        ReservaDetalleDTO reservaDetalle = convertirADetalleDTO(reservaActualizada);
        emailService.enviarNotificacionCambioEstado(reservaDetalle, reserva.getProveedor().getEmail());

        return reservaDetalle;
    }

    @Override
    @Transactional
    public void cancelarReserva(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con ID: " + id));

        // Solo se permite cancelar reservas en estado PENDIENTE
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new BadRequestException("Solo se pueden cancelar reservas en estado PENDIENTE");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepository.save(reserva);

        // Enviar notificación por correo sobre la cancelación
        ReservaDetalleDTO reservaDetalle = convertirADetalleDTO(reserva);
        emailService.enviarNotificacionCancelacion(reservaDetalle, reserva.getProveedor().getEmail());
    }
    private void validarTransicionEstado(EstadoReserva estadoActual, EstadoReserva nuevoEstado) {
        // Definir transiciones válidas
        switch (estadoActual) {
            case PENDIENTE:
                if (nuevoEstado != EstadoReserva.EN_PLANTA && nuevoEstado != EstadoReserva.CANCELADA) {
                    throw new BadRequestException("Desde PENDIENTE solo se puede pasar a EN_PLANTA o CANCELADA");
                }
                break;
            case EN_PLANTA:
                if (nuevoEstado != EstadoReserva.EN_RECEPCION) {
                    throw new BadRequestException("Desde EN_PLANTA solo se puede pasar a EN_RECEPCION");
                }
                break;
            case EN_RECEPCION:
                if (nuevoEstado != EstadoReserva.COMPLETADA) {
                    throw new BadRequestException("Desde EN_RECEPCION solo se puede pasar a COMPLETADA");
                }
                break;
            case COMPLETADA:
            case CANCELADA:
                throw new BadRequestException("No se puede cambiar el estado de una reserva COMPLETADA o CANCELADA");
            default:
                throw new BadRequestException("Estado no reconocido");
        }
    }

    private ReservaDTO convertirADTO(Reserva reserva) {
        ReservaDTO dto = new ReservaDTO();
        dto.setId(reserva.getId());
        dto.setProveedorId(reserva.getProveedor().getId());
        dto.setProveedorNombre(reserva.getProveedor().getNombre());
        dto.setAreaId(reserva.getArea().getId());
        dto.setAreaNombre(reserva.getArea().getNombre());
        dto.setAndenId(reserva.getAnden().getId());
        dto.setAndenNumero(reserva.getAnden().getNumero());
        dto.setTipoServicioId(reserva.getTipoServicio().getId());
        dto.setTipoServicioNombre(reserva.getTipoServicio().getNombre());
        dto.setFecha(reserva.getFecha());
        dto.setHoraInicio(reserva.getHoraInicio());
        dto.setHoraFin(reserva.getHoraFin());
        dto.setEstado(reserva.getEstado());
        dto.setTransportePlaca(reserva.getTransporte().getPlaca());
        return dto;
    }

    private ReservaDetalleDTO convertirADetalleDTO(Reserva reserva) {
        ReservaDetalleDTO dto = new ReservaDetalleDTO();
        dto.setId(reserva.getId());
        dto.setProveedorId(reserva.getProveedor().getId());
        dto.setProveedorNombre(reserva.getProveedor().getNombre());
        dto.setAreaId(reserva.getArea().getId());
        dto.setAreaNombre(reserva.getArea().getNombre());
        dto.setAndenId(reserva.getAnden().getId());
        dto.setAndenNumero(reserva.getAnden().getNumero());
        dto.setTipoServicioId(reserva.getTipoServicio().getId());
        dto.setTipoServicioNombre(reserva.getTipoServicio().getNombre());
        dto.setFecha(reserva.getFecha());
        dto.setHoraInicio(reserva.getHoraInicio());
        dto.setHoraFin(reserva.getHoraFin());
        dto.setEstado(reserva.getEstado());
        dto.setDescripcion(reserva.getDescripcion());

        // Datos del transporte
        dto.setTransporteTipo(reserva.getTransporte().getTipo());
        dto.setTransporteMarca(reserva.getTransporte().getMarca());
        dto.setTransporteModelo(reserva.getTransporte().getModelo());
        dto.setTransportePlaca(reserva.getTransporte().getPlaca());
        dto.setTransporteCapacidad(reserva.getTransporte().getCapacidad());

        // Datos de transportistas
        List<Transportista> transportistas = reserva.getTransporte().getTransportistas();

        // Conductor
        transportistas.stream()
                .filter(Transportista::getEsConductor)
                .findFirst()
                .ifPresent(conductor -> {
                    dto.setConductorNombres(conductor.getNombres());
                    dto.setConductorApellidos(conductor.getApellidos());
                    dto.setConductorCedula(conductor.getCedula());
                });

        // Ayudantes
        List<AyudanteDTO> ayudantesDTO = transportistas.stream()
                .filter(t -> !t.getEsConductor())
                .map(ayudante -> new AyudanteDTO(
                        ayudante.getNombres(),
                        ayudante.getApellidos(),
                        ayudante.getCedula()))
                .collect(Collectors.toList());

        dto.setAyudantes(ayudantesDTO);

        // Fechas de creación y actualización
        dto.setCreatedAt(reserva.getCreatedAt());
        dto.setUpdatedAt(reserva.getUpdatedAt());

        return dto;
    }
    @Override
    public ReservaDetalleDTO obtenerReservaPendienteProveedor(String username, LocalDate fecha) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));

        Proveedor proveedor = proveedorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado para el usuario: " + username));

        Optional<Reserva> reservaOpt = reservaRepository.findByProveedorAndFechaAndEstado(
                proveedor, fecha, EstadoReserva.PENDIENTE_CONFIRMACION);

        if (reservaOpt.isEmpty()) {
            throw new ResourceNotFoundException("No hay reserva pendiente de confirmación para la fecha: " + fecha);
        }

        return convertirADetalleDTO(reservaOpt.get());
    }

    @Override
    @Transactional
    public ReservaDetalleDTO completarDatosReserva(ReservaDTO reservaDTO, String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));

        Proveedor proveedor = proveedorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado para el usuario: " + username));

        // Buscar PRE-RESERVA pendiente para esa fecha
        Optional<Reserva> reservaOpt = reservaRepository.findByProveedorAndFechaAndEstado(
                proveedor, reservaDTO.getFecha(), EstadoReserva.PENDIENTE_CONFIRMACION);

        if (reservaOpt.isEmpty()) {
            throw new ResourceNotFoundException("No hay PRE-RESERVA pendiente para confirmar en la fecha: " + reservaDTO.getFecha());
        }

        Reserva reserva = reservaOpt.get();

        // ✅ VALIDAR QUE EL PROVEEDOR COMPLETE TODOS LOS DATOS OBLIGATORIOS
        validarDatosObligatoriosProveedor(reservaDTO);

        // 1. ACTUALIZAR ÁREA (proveedor debe elegir)
        Area area = areaRepository.findById(reservaDTO.getAreaId())
                .orElseThrow(() -> new ResourceNotFoundException("Área no encontrada con ID: " + reservaDTO.getAreaId()));

        // 2. ACTUALIZAR ANDÉN (proveedor debe elegir)
        Anden anden = andenRepository.findById(reservaDTO.getAndenId())
                .orElseThrow(() -> new ResourceNotFoundException("Andén no encontrado con ID: " + reservaDTO.getAndenId()));

        // Validar que el andén pertenezca al área
        if (!anden.getArea().getId().equals(area.getId())) {
            throw new BadRequestException("El andén seleccionado no pertenece al área especificada");
        }

        // 3. ACTUALIZAR TIPO DE SERVICIO (proveedor debe elegir)
        TipoServicio tipoServicio = tipoServicioRepository.findById(reservaDTO.getTipoServicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de servicio no encontrado con ID: " + reservaDTO.getTipoServicioId()));

        // 4. ACTUALIZAR DATOS DE TRANSPORTE (proveedor debe completar)
        Transporte transporte = reserva.getTransporte();
        transporte.setTipo(reservaDTO.getTransporteTipo());
        transporte.setMarca(reservaDTO.getTransporteMarca());
        transporte.setModelo(reservaDTO.getTransporteModelo());
        transporte.setPlaca(reservaDTO.getTransportePlaca());
        transporte.setCapacidad(reservaDTO.getTransporteCapacidad());
        transporteRepository.save(transporte);

        // 5. LIMPIAR Y CREAR TRANSPORTISTAS
        transportistaRepository.deleteAll(transporte.getTransportistas());

        List<Transportista> transportistas = new ArrayList<>();

        // Conductor (obligatorio)
        Transportista conductor = new Transportista();
        conductor.setTransporte(transporte);
        conductor.setNombres(reservaDTO.getConductorNombres());
        conductor.setApellidos(reservaDTO.getConductorApellidos());
        conductor.setCedula(reservaDTO.getConductorCedula());
        conductor.setEsConductor(true);
        transportistas.add(conductor);

        // Ayudantes (opcional)
        if (reservaDTO.getAyudantes() != null && !reservaDTO.getAyudantes().isEmpty()) {
            for (AyudanteDTO ayudanteDTO : reservaDTO.getAyudantes()) {
                Transportista ayudante = new Transportista();
                ayudante.setTransporte(transporte);
                ayudante.setNombres(ayudanteDTO.getNombres());
                ayudante.setApellidos(ayudanteDTO.getApellidos());
                ayudante.setCedula(ayudanteDTO.getCedula());
                ayudante.setEsConductor(false);
                transportistas.add(ayudante);
            }
        }
        transportistaRepository.saveAll(transportistas);

        // 6. ACTUALIZAR RESERVA CON TODOS LOS DATOS
        reserva.setArea(area);
        reserva.setAnden(anden);
        reserva.setTipoServicio(tipoServicio);

        // Mantener datos de la plantilla (fecha, horarios ya están)
        // Agregar descripción con palets si se proporcionó
        String descripcion = "Reserva confirmada";
        if (reservaDTO.getNumeroPalets() != null) {
            descripcion += " - Palets: " + reservaDTO.getNumeroPalets();
        }
        if (reservaDTO.getDescripcion() != null && !reservaDTO.getDescripcion().trim().isEmpty()) {
            descripcion += " - " + reservaDTO.getDescripcion();
        }
        reserva.setDescripcion(descripcion);

        // ✅ CAMBIAR ESTADO A CONFIRMADA
        reserva.setEstado(EstadoReserva.CONFIRMADA);

        Reserva reservaActualizada = reservaRepository.save(reserva);

        // Enviar notificación
        ReservaDetalleDTO reservaDetalle = convertirADetalleDTO(reservaActualizada);
        emailService.enviarConfirmacionReserva(reservaDetalle, proveedor.getEmail());

        log.info("PRE-RESERVA {} confirmada completamente por proveedor {}",
                reserva.getId(), proveedor.getNombre());

        return reservaDetalle;
    }


    @Override
    @Transactional
    public ReservaDetalleDTO actualizarDatosTransporteReserva(Long id, ReservaDTO reservaDTO, String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));

        Proveedor proveedor = proveedorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado para el usuario: " + username));

        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con ID: " + id));

        // Verificar que la reserva pertenece al proveedor
        if (!reserva.getProveedor().getId().equals(proveedor.getId())) {
            throw new BadRequestException("No tiene permisos para editar esta reserva");
        }

        // Solo puede editar si está en CONFIRMADA o PENDIENTE_CONFIRMACION
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA &&
                reserva.getEstado() != EstadoReserva.PENDIENTE_CONFIRMACION) {
            throw new BadRequestException("Solo se pueden editar reservas confirmadas o pendientes de confirmación");
        }

        // Actualizar solo datos de transporte
        Transporte transporte = reserva.getTransporte();
        transporte.setTipo(reservaDTO.getTransporteTipo());
        transporte.setMarca(reservaDTO.getTransporteMarca());
        transporte.setModelo(reservaDTO.getTransporteModelo());
        transporte.setPlaca(reservaDTO.getTransportePlaca());
        transporte.setCapacidad(reservaDTO.getTransporteCapacidad());

        transporteRepository.save(transporte);

        // Actualizar transportistas
        transportistaRepository.deleteAll(transporte.getTransportistas());

        List<Transportista> transportistas = new ArrayList<>();

        // Conductor
        Transportista conductor = new Transportista();
        conductor.setTransporte(transporte);
        conductor.setNombres(reservaDTO.getConductorNombres());
        conductor.setApellidos(reservaDTO.getConductorApellidos());
        conductor.setCedula(reservaDTO.getConductorCedula());
        conductor.setEsConductor(true);
        transportistas.add(conductor);

        // Ayudantes
        if (reservaDTO.getAyudantes() != null && !reservaDTO.getAyudantes().isEmpty()) {
            for (AyudanteDTO ayudanteDTO : reservaDTO.getAyudantes()) {
                Transportista ayudante = new Transportista();
                ayudante.setTransporte(transporte);
                ayudante.setNombres(ayudanteDTO.getNombres());
                ayudante.setApellidos(ayudanteDTO.getApellidos());
                ayudante.setCedula(ayudanteDTO.getCedula());
                ayudante.setEsConductor(false);
                transportistas.add(ayudante);
            }
        }

        transportistaRepository.saveAll(transportistas);

        // Actualizar descripción con palets
        if (reservaDTO.getNumeroPalets() != null) {
            reserva.setDescripcion("Palets: " + reservaDTO.getNumeroPalets() +
                    (reservaDTO.getDescripcion() != null ? " - " + reservaDTO.getDescripcion() : ""));
        }

        Reserva reservaActualizada = reservaRepository.save(reserva);

        return convertirADetalleDTO(reservaActualizada);
    }

    @Override
    @Transactional
    public void cancelarReservaProveedor(Long id, String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));

        Proveedor proveedor = proveedorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado para el usuario: " + username));

        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con ID: " + id));

        // Verificar que la reserva pertenece al proveedor
        if (!reserva.getProveedor().getId().equals(proveedor.getId())) {
            throw new BadRequestException("No tiene permisos para cancelar esta reserva");
        }

        // Solo puede cancelar si está en CONFIRMADA o PENDIENTE_CONFIRMACION
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA &&
                reserva.getEstado() != EstadoReserva.PENDIENTE_CONFIRMACION) {
            throw new BadRequestException("Solo se pueden cancelar reservas confirmadas o pendientes de confirmación");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepository.save(reserva);

        // Enviar notificación
        ReservaDetalleDTO reservaDetalle = convertirADetalleDTO(reserva);
        emailService.enviarNotificacionCancelacion(reservaDetalle, proveedor.getEmail());
    }

    @Override
    public List<ReservaDTO> obtenerReservasDeProveedor(String username, LocalDate fechaInicio, LocalDate fechaFin) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));

        Proveedor proveedor = proveedorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado para el usuario: " + username));

        List<Reserva> reservas;

        if (fechaInicio != null && fechaFin != null) {
            reservas = reservaRepository.findByProveedorAndFechaBetween(proveedor, fechaInicio, fechaFin);
        } else {
            reservas = reservaRepository.findByProveedor(proveedor);
        }

        return reservas.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    public Object obtenerEstadisticasProveedor(String username, LocalDate fechaInicio, LocalDate fechaFin) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));

        Proveedor proveedor = proveedorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado para el usuario: " + username));

        List<Reserva> reservas;

        if (fechaInicio != null && fechaFin != null) {
            reservas = reservaRepository.findByProveedorAndFechaBetween(proveedor, fechaInicio, fechaFin);
        } else {
            // Por defecto, últimos 30 días
            LocalDate hace30Dias = LocalDate.now().minusDays(30);
            reservas = reservaRepository.findByProveedorAndFechaBetween(proveedor, hace30Dias, LocalDate.now());
        }

        // Calcular estadísticas
        long totalReservas = reservas.size();
        long completadas = reservas.stream().mapToLong(r -> r.getEstado() == EstadoReserva.COMPLETADA ? 1 : 0).sum();
        long canceladas = reservas.stream().mapToLong(r -> r.getEstado() == EstadoReserva.CANCELADA ? 1 : 0).sum();
        long pendientes = reservas.stream().mapToLong(r -> r.getEstado() == EstadoReserva.PENDIENTE_CONFIRMACION ? 1 : 0).sum();
        long confirmadas = reservas.stream().mapToLong(r -> r.getEstado() == EstadoReserva.CONFIRMADA ? 1 : 0).sum();

        Map<EstadoReserva, Long> porEstado = reservas.stream()
                .collect(Collectors.groupingBy(Reserva::getEstado, Collectors.counting()));

        return Map.of(
                "totalReservas", totalReservas,
                "completadas", completadas,
                "canceladas", canceladas,
                "pendientesConfirmacion", pendientes,
                "confirmadas", confirmadas,
                "distribucionPorEstado", porEstado,
                "periodoConsultado", Map.of(
                        "fechaInicio", fechaInicio != null ? fechaInicio : LocalDate.now().minusDays(30),
                        "fechaFin", fechaFin != null ? fechaFin : LocalDate.now()
                )
        );
    }

    private void validarDatosObligatoriosProveedor(ReservaDTO reservaDTO) {
        List<String> errores = new ArrayList<>();

        // Validar área
        if (reservaDTO.getAreaId() == null) {
            errores.add("Debe seleccionar un área");
        }

        // Validar andén
        if (reservaDTO.getAndenId() == null) {
            errores.add("Debe seleccionar un andén");
        }

        // Validar tipo de servicio
        if (reservaDTO.getTipoServicioId() == null) {
            errores.add("Debe seleccionar un tipo de servicio");
        }

        // Validar datos de transporte
        if (reservaDTO.getTransporteTipo() == null || reservaDTO.getTransporteTipo().trim().isEmpty()) {
            errores.add("Debe especificar el tipo de transporte");
        }
        if (reservaDTO.getTransporteMarca() == null || reservaDTO.getTransporteMarca().trim().isEmpty()) {
            errores.add("Debe especificar la marca del transporte");
        }
        if (reservaDTO.getTransporteModelo() == null || reservaDTO.getTransporteModelo().trim().isEmpty()) {
            errores.add("Debe especificar el modelo del transporte");
        }
        if (reservaDTO.getTransportePlaca() == null || reservaDTO.getTransportePlaca().trim().isEmpty()) {
            errores.add("Debe especificar la placa del transporte");
        }

        // Validar datos del conductor
        if (reservaDTO.getConductorNombres() == null || reservaDTO.getConductorNombres().trim().isEmpty()) {
            errores.add("Debe especificar los nombres del conductor");
        }
        if (reservaDTO.getConductorApellidos() == null || reservaDTO.getConductorApellidos().trim().isEmpty()) {
            errores.add("Debe especificar los apellidos del conductor");
        }
        if (reservaDTO.getConductorCedula() == null || reservaDTO.getConductorCedula().trim().isEmpty()) {
            errores.add("Debe especificar la cédula del conductor");
        }

        if (!errores.isEmpty()) {
            throw new BadRequestException("Faltan datos obligatorios: " + String.join(", ", errores));
        }
    }
}