package com.logistica.agendamiento.controller;

import com.logistica.agendamiento.dto.ProveedorDTO;
import com.logistica.agendamiento.service.ProveedorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GUARDIA', 'AGENTE')")
    public ResponseEntity<List<ProveedorDTO>> obtenerTodosLosProveedores() {
        return ResponseEntity.ok(proveedorService.obtenerTodosLosProveedores());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GUARDIA', 'AGENTE', 'PROVEEDOR')")
    public ResponseEntity<ProveedorDTO> obtenerProveedorPorId(@PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.obtenerProveedorPorId(id));
    }

    @GetMapping("/ruc/{ruc}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GUARDIA', 'AGENTE')")
    public ResponseEntity<ProveedorDTO> obtenerProveedorPorRuc(@PathVariable String ruc) {
        return ResponseEntity.ok(proveedorService.obtenerProveedorPorRuc(ruc));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVEEDOR')")
    public ResponseEntity<ProveedorDTO> actualizarProveedor(
            @PathVariable Long id,
            @RequestBody ProveedorDTO proveedorDTO) {
        return ResponseEntity.ok(proveedorService.actualizarProveedor(id, proveedorDTO));
    }

    @PatchMapping("/{id}/cambiar-estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProveedorDTO> cambiarEstadoProveedor(
            @PathVariable Long id,
            @RequestParam Boolean estado) {
        return ResponseEntity.ok(proveedorService.cambiarEstadoProveedor(id, estado));
    }
    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVEEDOR')")
    public ResponseEntity<ProveedorDTO> obtenerProveedorPorUsuarioId(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(proveedorService.obtenerProveedorPorUsuarioId(usuarioId));
    }
}