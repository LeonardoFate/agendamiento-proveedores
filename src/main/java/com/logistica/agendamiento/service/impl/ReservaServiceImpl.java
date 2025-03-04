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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;
    private final ProveedorRepository proveedorRepository;
    private final AreaRepository areaRepository;
    private final AndenRepository andenRepository;
    private final TipoServicioRepository tipoServicioRepository;
    private final TransporteRepository transporteRepository;
    private final TransportistaRepository transportistaRepository;
    private final EmailService emailService;

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

        // Validar si el andén es adecuado para el tipo de servicio
        boolean esContenedor = tipoServicio.getNombre().equalsIgnoreCase("Contenedor");
        if (esContenedor && !anden.getExclusivoContenedor() || !esContenedor && anden.getExclusivoContenedor()) {
            throw new BadRequestException("El andén seleccionado no es compatible con el tipo de servicio");
        }

        // Verificar disponibilidad del andén
        if (anden.getEstado() != EstadoAnden.DISPONIBLE) {
            throw new BadRequestException("El andén seleccionado no está disponible actualmente");
        }

        // Verificar si hay conflictos con otras reservas
        List<Reserva> conflictos = reservaRepository.findConflictos(
                reservaDTO.getFecha(),
                anden,
                reservaDTO.getHoraInicio(),
                reservaDTO.getHoraFin());

        if (!conflictos.isEmpty()) {
            throw new BadRequestException("El horario seleccionado no está disponible para este andén");
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

        // Verificar disponibilidad del andén para la nueva fecha/hora
        if (!reserva.getAnden().getId().equals(anden.getId()) ||
                !reserva.getFecha().equals(reservaDTO.getFecha()) ||
                !reserva.getHoraInicio().equals(reservaDTO.getHoraInicio()) ||
                !reserva.getHoraFin().equals(reservaDTO.getHoraFin())) {

            // Verificar si hay conflictos con otras reservas
            List<Reserva> conflictos = reservaRepository.findConflictos(
                    reservaDTO.getFecha(),
                    anden,
                    reservaDTO.getHoraInicio(),
                    reservaDTO.getHoraFin());

            // Excluir la reserva actual de los conflictos
            conflictos = conflictos.stream()
                    .filter(r -> !r.getId().equals(id))
                    .collect(Collectors.toList());

            if (!conflictos.isEmpty()) {
                throw new BadRequestException("El horario seleccionado no está disponible para este andén");
            }
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

    @Override
    public List<DisponibilidadAndenDTO> obtenerDisponibilidadPorFechaYArea(LocalDate fecha, Long areaId, Long tipoServicioId) {
        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("Área no encontrada con ID: " + areaId));

        // Determinar si se requieren andenes exclusivos para contenedores
        boolean requiereContenedor = false;
        if (tipoServicioId != null) {
            TipoServicio tipoServicio = tipoServicioRepository.findById(tipoServicioId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de servicio no encontrado con ID: " + tipoServicioId));
            requiereContenedor = tipoServicio.getNombre().equalsIgnoreCase("Contenedor");
        }

        // Obtener todos los andenes del área que coincidan con el requisito de contenedor
        List<Anden> andenes;
        if (tipoServicioId != null) {
            andenes = andenRepository.findByAreaAndExclusivoContenedor(area, requiereContenedor);
        } else {
            andenes = andenRepository.findByArea(area);
        }

        // Obtener todas las reservas para esa fecha y área
        List<Reserva> reservasDelDia = reservaRepository.findByFechaAndArea(fecha, area);

        // Generar disponibilidad para cada andén
        return andenes.stream().map(anden -> {
            DisponibilidadAndenDTO disponibilidad = new DisponibilidadAndenDTO();
            disponibilidad.setAndenId(anden.getId());
            disponibilidad.setNumero(anden.getNumero());
            disponibilidad.setAreaId(area.getId());
            disponibilidad.setAreaNombre(area.getNombre());
            disponibilidad.setEstadoActual(anden.getEstado());
            disponibilidad.setExclusivoContenedor(anden.getExclusivoContenedor());

            // Filtrar reservas para este andén específico
            List<HorarioReservadoDTO> horariosReservados = reservasDelDia.stream()
                    .filter(r -> r.getAnden().getId().equals(anden.getId()))
                    .filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
                    .map(r -> new HorarioReservadoDTO(r.getHoraInicio(), r.getHoraFin()))
                    .collect(Collectors.toList());

            disponibilidad.setHorariosReservados(horariosReservados);

            return disponibilidad;
        }).collect(Collectors.toList());
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
}