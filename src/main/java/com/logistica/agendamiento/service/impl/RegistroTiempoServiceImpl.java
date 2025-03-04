package com.logistica.agendamiento.service.impl;

import com.logistica.agendamiento.dto.RegistroTiempoDTO;
import com.logistica.agendamiento.entity.RegistroTiempo;
import com.logistica.agendamiento.entity.Reserva;
import com.logistica.agendamiento.entity.Usuario;
import com.logistica.agendamiento.entity.enums.TipoRegistro;
import com.logistica.agendamiento.exception.BadRequestException;
import com.logistica.agendamiento.exception.ResourceNotFoundException;
import com.logistica.agendamiento.repository.RegistroTiempoRepository;
import com.logistica.agendamiento.repository.ReservaRepository;
import com.logistica.agendamiento.repository.UsuarioRepository;
import com.logistica.agendamiento.service.RegistroTiempoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistroTiempoServiceImpl implements RegistroTiempoService {

    private final RegistroTiempoRepository registroTiempoRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    public List<RegistroTiempoDTO> obtenerRegistrosPorReservaId(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con ID: " + reservaId));

        return registroTiempoRepository.findByReserva(reserva).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RegistroTiempoDTO iniciarRegistro(Long reservaId, Long usuarioId, TipoRegistro tipo) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con ID: " + reservaId));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        // Verificar si ya existe un registro del mismo tipo sin finalizar
        registroTiempoRepository.findByReservaAndTipoAndHoraFinIsNull(reserva, tipo)
                .ifPresent(registro -> {
                    throw new BadRequestException("Ya existe un registro de " + tipo + " sin finalizar para esta reserva");
                });

        RegistroTiempo registro = new RegistroTiempo();
        registro.setReserva(reserva);
        registro.setUsuario(usuario);
        registro.setTipo(tipo);
        registro.setHoraInicio(LocalDateTime.now());

        RegistroTiempo registroSaved = registroTiempoRepository.save(registro);
        return convertirADTO(registroSaved);
    }

    @Override
    @Transactional
    public RegistroTiempoDTO finalizarRegistro(Long registroId) {
        RegistroTiempo registro = registroTiempoRepository.findById(registroId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de tiempo no encontrado con ID: " + registroId));

        // Verificar que el registro no esté ya finalizado
        if (registro.getHoraFin() != null) {
            throw new BadRequestException("El registro ya fue finalizado");
        }

        LocalDateTime horaFin = LocalDateTime.now();
        registro.setHoraFin(horaFin);

        // Calcular duración en segundos
        Duration duracion = Duration.between(registro.getHoraInicio(), horaFin);
        registro.setDuracion((int) duracion.getSeconds());

        RegistroTiempo registroActualizado = registroTiempoRepository.save(registro);
        return convertirADTO(registroActualizado);
    }

    @Override
    public List<RegistroTiempoDTO> obtenerRegistrosPorFecha(LocalDate fecha) {
        LocalDateTime fechaInicio = fecha.atStartOfDay();
        LocalDateTime fechaFin = fecha.atTime(LocalTime.MAX);

        return registroTiempoRepository.findByRangoFechas(fechaInicio, fechaFin).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    public Double obtenerTiempoPromedioByTipo(TipoRegistro tipo) {
        return registroTiempoRepository.findPromedioDuracionByTipo(tipo);
    }

    private RegistroTiempoDTO convertirADTO(RegistroTiempo registro) {
        RegistroTiempoDTO dto = new RegistroTiempoDTO();
        dto.setId(registro.getId());
        dto.setReservaId(registro.getReserva().getId());
        dto.setProveedorNombre(registro.getReserva().getProveedor().getNombre());
        dto.setUsuarioId(registro.getUsuario().getId());
        dto.setUsuarioNombre(registro.getUsuario().getNombre() + " " + registro.getUsuario().getApellido());
        dto.setTipo(registro.getTipo());
        dto.setHoraInicio(registro.getHoraInicio());
        dto.setHoraFin(registro.getHoraFin());
        dto.setDuracion(registro.getDuracion());
        return dto;
    }
}