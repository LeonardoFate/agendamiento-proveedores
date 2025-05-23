package com.logistica.agendamiento.service;

import com.logistica.agendamiento.dto.ReservaDTO;
import com.logistica.agendamiento.dto.ReservaDetalleDTO;
import com.logistica.agendamiento.entity.enums.EstadoReserva;

import java.time.LocalDate;
import java.util.List;

public interface ReservaService {

    List<ReservaDTO> obtenerTodasLasReservas();

    List<ReservaDTO> obtenerReservasPorFecha(LocalDate fecha);

    List<ReservaDTO> obtenerReservasPorProveedorId(Long proveedorId);

    List<ReservaDTO> obtenerReservasPorAreaId(Long areaId);

    List<ReservaDTO> obtenerReservasPorAndenId(Long andenId);

    List<ReservaDTO> obtenerReservasPorEstado(EstadoReserva estado);

    ReservaDetalleDTO obtenerReservaPorId(Long id);

    ReservaDetalleDTO crearReserva(ReservaDTO reservaDTO);

    ReservaDetalleDTO actualizarReserva(Long id, ReservaDTO reservaDTO);

    ReservaDetalleDTO actualizarEstadoReserva(Long id, EstadoReserva estado);

    void cancelarReserva(Long id);

    // ✅ NUEVOS MÉTODOS PARA PROVEEDORES
    ReservaDetalleDTO obtenerReservaPendienteProveedor(String username, LocalDate fecha);

    ReservaDetalleDTO completarDatosReserva(ReservaDTO reservaDTO, String username);

    ReservaDetalleDTO actualizarDatosTransporteReserva(Long id, ReservaDTO reservaDTO, String username);

    void cancelarReservaProveedor(Long id, String username);

    List<ReservaDTO> obtenerReservasDeProveedor(String username, LocalDate fechaInicio, LocalDate fechaFin);

    Object obtenerEstadisticasProveedor(String username, LocalDate fechaInicio, LocalDate fechaFin);

}