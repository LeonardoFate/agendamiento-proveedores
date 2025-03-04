package com.logistica.agendamiento.controller;

import com.logistica.agendamiento.dto.DisponibilidadAndenDTO;
import com.logistica.agendamiento.dto.ReservaDTO;
import com.logistica.agendamiento.dto.ReservaDetalleDTO;
import com.logistica.agendamiento.entity.enums.EstadoReserva;
import com.logistica.agendamiento.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

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

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVEEDOR')")
    public ResponseEntity<ReservaDetalleDTO> crearReserva(@Valid @RequestBody ReservaDTO reservaDTO) {
        return new ResponseEntity<>(reservaService.crearReserva(reservaDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVEEDOR')")
    public ResponseEntity<ReservaDetalleDTO> actualizarReserva(
            @PathVariable Long id,
            @Valid @RequestBody ReservaDTO reservaDTO) {
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
    public ResponseEntity<Void> cancelarReserva(@PathVariable Long id) {
        reservaService.cancelarReserva(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<List<DisponibilidadAndenDTO>> obtenerDisponibilidadPorFechaYArea(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam Long areaId,
            @RequestParam(required = false) Long tipoServicioId) {
        return ResponseEntity.ok(reservaService.obtenerDisponibilidadPorFechaYArea(fecha, areaId, tipoServicioId));
    }
}