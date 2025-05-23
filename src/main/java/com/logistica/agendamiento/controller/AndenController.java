package com.logistica.agendamiento.controller;

import com.logistica.agendamiento.dto.AndenDTO;
import com.logistica.agendamiento.entity.enums.EstadoAnden;
import com.logistica.agendamiento.service.AndenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/andenes")
@RequiredArgsConstructor
public class AndenController {

    private final AndenService andenService;

    @GetMapping
    public ResponseEntity<List<AndenDTO>> obtenerTodosLosAndenes() {
        return ResponseEntity.ok(andenService.obtenerTodosLosAndenes());
    }

    @GetMapping("/area/{areaId}")
    public ResponseEntity<List<AndenDTO>> obtenerAndenesPorArea(@PathVariable Long areaId) {
        return ResponseEntity.ok(andenService.obtenerAndenesPorArea(areaId));
    }

    @GetMapping("/area/{areaId}/estado/{estado}")
    public ResponseEntity<List<AndenDTO>> obtenerAndenesPorAreaYEstado(
            @PathVariable Long areaId,
            @PathVariable EstadoAnden estado) {
        return ResponseEntity.ok(andenService.obtenerAndenesPorAreaYEstado(areaId, estado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AndenDTO> obtenerAndenPorId(@PathVariable Long id) {
        return ResponseEntity.ok(andenService.obtenerAndenPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AndenDTO> crearAnden(@Valid @RequestBody AndenDTO andenDTO) {
        return new ResponseEntity<>(andenService.crearAnden(andenDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AndenDTO> actualizarAnden(
            @PathVariable Long id,
            @Valid @RequestBody AndenDTO andenDTO) {
        return ResponseEntity.ok(andenService.actualizarAnden(id, andenDTO));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENTE')")
    public ResponseEntity<AndenDTO> actualizarEstadoAnden(
            @PathVariable Long id,
            @RequestParam EstadoAnden estado) {
        return ResponseEntity.ok(andenService.actualizarEstadoAnden(id, estado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarAnden(@PathVariable Long id) {
        andenService.eliminarAnden(id);
        return ResponseEntity.noContent().build();
    }
}