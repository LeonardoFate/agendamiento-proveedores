package com.logistica.agendamiento.controller;

import com.logistica.agendamiento.dto.HorarioProveedorDTO;
import com.logistica.agendamiento.dto.ReservaDTO;
import com.logistica.agendamiento.dto.ReservaDetalleDTO;
import com.logistica.agendamiento.entity.enums.EstadoReserva;
import com.logistica.agendamiento.service.PlantillaHorarioService;
import com.logistica.agendamiento.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;
    private final PlantillaHorarioService plantillaHorarioService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GUARDIA', 'AGENTE')")
    public ResponseEntity<List<ReservaDTO>> obtenerTodasLasReservas() {
        return ResponseEntity.ok(reservaService.obtenerTodasLasReservas());
    }

    @GetMapping("/fecha/{fecha}")
    public ResponseEntity<List<ReservaDTO>> obtenerReservasPorFecha(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(reservaService.obtenerReservasPorFecha(fecha));
    }

    @GetMapping("/proveedor/{proveedorId}")
    public ResponseEntity<List<ReservaDTO>> obtenerReservasPorProveedorId(@PathVariable Long proveedorId) {
        return ResponseEntity.ok(reservaService.obtenerReservasPorProveedorId(proveedorId));
    }

    @GetMapping("/area/{areaId}")
    public ResponseEntity<List<ReservaDTO>> obtenerReservasPorAreaId(@PathVariable Long areaId) {
        return ResponseEntity.ok(reservaService.obtenerReservasPorAreaId(areaId));
    }

    @GetMapping("/anden/{andenId}")
    public ResponseEntity<List<ReservaDTO>> obtenerReservasPorAndenId(@PathVariable Long andenId) {
        return ResponseEntity.ok(reservaService.obtenerReservasPorAndenId(andenId));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<ReservaDTO>> obtenerReservasPorEstado(@PathVariable EstadoReserva estado) {
        return ResponseEntity.ok(reservaService.obtenerReservasPorEstado(estado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservaDetalleDTO> obtenerReservaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.obtenerReservaPorId(id));
    }

    // ✅ NUEVO: Obtener horario asignado del proveedor para una fecha específica
    @GetMapping("/mi-horario")
    @PreAuthorize("hasRole('PROVEEDOR')")
    public ResponseEntity<HorarioProveedorDTO> obtenerMiHorario(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Authentication authentication) {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        HorarioProveedorDTO horario = plantillaHorarioService.obtenerHorarioProveedorPorFecha(username, fecha);

        return ResponseEntity.ok(horario);
    }

    // ✅ NUEVO: Obtener horarios de la semana completa del proveedor
    @GetMapping("/mi-horario-semanal")
    @PreAuthorize("hasRole('PROVEEDOR')")
    public ResponseEntity<List<HorarioProveedorDTO>> obtenerMiHorarioSemanal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            Authentication authentication) {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        List<HorarioProveedorDTO> horariosSemana = plantillaHorarioService.obtenerHorarioProveedorSemana(username, fechaInicio);

        return ResponseEntity.ok(horariosSemana);
    }

    // ✅ NUEVO: Obtener reserva pendiente del proveedor para una fecha
    @GetMapping("/mi-reserva-pendiente")
    @PreAuthorize("hasRole('PROVEEDOR')")
    public ResponseEntity<ReservaDetalleDTO> obtenerMiReservaPendiente(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Authentication authentication) {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        ReservaDetalleDTO reservaPendiente = reservaService.obtenerReservaPendienteProveedor(username, fecha);

        return ResponseEntity.ok(reservaPendiente);
    }

    // ✅ NUEVO: Confirmar reserva (proveedor completa datos de reserva pre-creada)
    @PostMapping("/confirmar")
    @PreAuthorize("hasRole('PROVEEDOR')")
    public ResponseEntity<ReservaDetalleDTO> confirmarReserva(
            @Valid @RequestBody ReservaDTO reservaDTO,
            Authentication authentication) {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        ReservaDetalleDTO reserva = reservaService.completarDatosReserva(reservaDTO, username);
        return new ResponseEntity<>(reserva, HttpStatus.CREATED);
    }

    // ✅ MANTENER: Crear reserva (solo para administradores)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservaDetalleDTO> crearReserva(@Valid @RequestBody ReservaDTO reservaDTO) {
        return new ResponseEntity<>(reservaService.crearReserva(reservaDTO), HttpStatus.CREATED);
    }

    // ✅ MODIFICADO: Actualizar reserva (proveedores solo pueden editar datos de transporte)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVEEDOR')")
    public ResponseEntity<ReservaDetalleDTO> actualizarReserva(
            @PathVariable Long id,
            @Valid @RequestBody ReservaDTO reservaDTO,
            Authentication authentication) {

        if (authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_PROVEEDOR"))) {

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();

            ReservaDetalleDTO reserva = reservaService.actualizarDatosTransporteReserva(id, reservaDTO, username);
            return ResponseEntity.ok(reserva);
        }

        return ResponseEntity.ok(reservaService.actualizarReserva(id, reservaDTO));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'GUARDIA', 'AGENTE')")
    public ResponseEntity<ReservaDetalleDTO> actualizarEstadoReserva(
            @PathVariable Long id,
            @RequestParam EstadoReserva estado) {
        return ResponseEntity.ok(reservaService.actualizarEstadoReserva(id, estado));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVEEDOR')")
    public ResponseEntity<Void> cancelarReserva(
            @PathVariable Long id,
            Authentication authentication) {

        if (authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_PROVEEDOR"))) {

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();

            reservaService.cancelarReservaProveedor(id, username);
        } else {
            reservaService.cancelarReserva(id);
        }

        return ResponseEntity.noContent().build();
    }

    // ✅ NUEVO: Obtener mis reservas como proveedor
    @GetMapping("/mis-reservas")
    @PreAuthorize("hasRole('PROVEEDOR')")
    public ResponseEntity<List<ReservaDTO>> obtenerMisReservas(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        List<ReservaDTO> misReservas = reservaService.obtenerReservasDeProveedor(username, fechaInicio, fechaFin);

        return ResponseEntity.ok(misReservas);
    }

    // ✅ NUEVO: Obtener estadísticas de mis reservas
    @GetMapping("/mis-estadisticas")
    @PreAuthorize("hasRole('PROVEEDOR')")
    public ResponseEntity<?> obtenerMisEstadisticas(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        Object estadisticas = reservaService.obtenerEstadisticasProveedor(username, fechaInicio, fechaFin);

        return ResponseEntity.ok(estadisticas);
    }
}
