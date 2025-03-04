package com.logistica.agendamiento.controller;

import com.logistica.agendamiento.dto.RegistroTiempoDTO;
import com.logistica.agendamiento.entity.enums.TipoRegistro;
import com.logistica.agendamiento.service.RegistroTiempoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/registros-tiempo")
@RequiredArgsConstructor
public class RegistroTiempoController {

    private final RegistroTiempoService registroTiempoService;

    @GetMapping("/reserva/{reservaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GUARDIA', 'AGENTE')")
    public ResponseEntity<List<RegistroTiempoDTO>> obtenerRegistrosPorReservaId(@PathVariable Long reservaId) {
        return ResponseEntity.ok(registroTiempoService.obtenerRegistrosPorReservaId(reservaId));
    }

    @PostMapping("/iniciar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GUARDIA', 'AGENTE')")
    public ResponseEntity<RegistroTiempoDTO> iniciarRegistro(
            @RequestParam Long reservaId,
            @RequestParam Long usuarioId,
            @RequestParam TipoRegistro tipo) {
        return new ResponseEntity<>(registroTiempoService.iniciarRegistro(reservaId, usuarioId, tipo), HttpStatus.CREATED);
    }

    @PostMapping("/{registroId}/finalizar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GUARDIA', 'AGENTE')")
    public ResponseEntity<RegistroTiempoDTO> finalizarRegistro(@PathVariable Long registroId) {
        return ResponseEntity.ok(registroTiempoService.finalizarRegistro(registroId));
    }

    @GetMapping("/fecha/{fecha}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GUARDIA', 'AGENTE')")
    public ResponseEntity<List<RegistroTiempoDTO>> obtenerRegistrosPorFecha(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(registroTiempoService.obtenerRegistrosPorFecha(fecha));
    }

    @GetMapping("/promedio")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Double> obtenerTiempoPromedioByTipo(@RequestParam TipoRegistro tipo) {
        return ResponseEntity.ok(registroTiempoService.obtenerTiempoPromedioByTipo(tipo));
    }
}