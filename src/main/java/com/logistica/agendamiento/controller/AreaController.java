package com.logistica.agendamiento.controller;

import com.logistica.agendamiento.dto.AreaDTO;
import com.logistica.agendamiento.service.AreaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/areas")
@RequiredArgsConstructor
public class AreaController {

    private final AreaService areaService;

    @GetMapping
    public ResponseEntity<List<AreaDTO>> obtenerTodasLasAreas() {
        return ResponseEntity.ok(areaService.obtenerTodasLasAreas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AreaDTO> obtenerAreaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(areaService.obtenerAreaPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AreaDTO> crearArea(@Valid @RequestBody AreaDTO areaDTO) {
        return new ResponseEntity<>(areaService.crearArea(areaDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AreaDTO> actualizarArea(
            @PathVariable Long id,
            @Valid @RequestBody AreaDTO areaDTO) {
        return ResponseEntity.ok(areaService.actualizarArea(id, areaDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarArea(@PathVariable Long id) {
        areaService.eliminarArea(id);
        return ResponseEntity.noContent().build();
    }
}