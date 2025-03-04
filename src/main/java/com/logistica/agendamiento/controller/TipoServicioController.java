package com.logistica.agendamiento.controller;

import com.logistica.agendamiento.dto.TipoServicioDTO;
import com.logistica.agendamiento.service.TipoServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-servicio")
@RequiredArgsConstructor
public class TipoServicioController {

    private final TipoServicioService tipoServicioService;

    @GetMapping
    public ResponseEntity<List<TipoServicioDTO>> obtenerTodosTiposServicio() {
        return ResponseEntity.ok(tipoServicioService.obtenerTodosTiposServicio());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoServicioDTO> obtenerTipoServicioPorId(@PathVariable Long id) {
        return ResponseEntity.ok(tipoServicioService.obtenerTipoServicioPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TipoServicioDTO> crearTipoServicio(@Valid @RequestBody TipoServicioDTO tipoServicioDTO) {
        return new ResponseEntity<>(tipoServicioService.crearTipoServicio(tipoServicioDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TipoServicioDTO> actualizarTipoServicio(
            @PathVariable Long id,
            @Valid @RequestBody TipoServicioDTO tipoServicioDTO) {
        return ResponseEntity.ok(tipoServicioService.actualizarTipoServicio(id, tipoServicioDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarTipoServicio(@PathVariable Long id) {
        tipoServicioService.eliminarTipoServicio(id);
        return ResponseEntity.noContent().build();
    }
}