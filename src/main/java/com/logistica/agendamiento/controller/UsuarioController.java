package com.logistica.agendamiento.controller;

import com.logistica.agendamiento.dto.UsuarioDTO;
import com.logistica.agendamiento.entity.enums.Rol;
import com.logistica.agendamiento.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> obtenerTodosLosUsuarios() {
        return ResponseEntity.ok(usuarioService.obtenerTodosLosUsuarios());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> obtenerUsuarioPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerUsuarioPorId(id));
    }

    @GetMapping("/rol/{rol}")
    public ResponseEntity<List<UsuarioDTO>> obtenerUsuariosPorRol(@PathVariable Rol rol) {
        return ResponseEntity.ok(usuarioService.obtenerUsuariosPorRol(rol));
    }

    @PostMapping
    public ResponseEntity<UsuarioDTO> crearUsuario(@Valid @RequestBody UsuarioDTO usuarioDTO) {
        return new ResponseEntity<>(usuarioService.crearUsuario(usuarioDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> actualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioDTO usuarioDTO) {
        return ResponseEntity.ok(usuarioService.actualizarUsuario(id, usuarioDTO));
    }

    @PatchMapping("/{id}/cambiar-estado")
    public ResponseEntity<UsuarioDTO> cambiarEstadoUsuario(
            @PathVariable Long id,
            @RequestParam Boolean estado) {
        return ResponseEntity.ok(usuarioService.cambiarEstadoUsuario(id, estado));
    }

    @PatchMapping("/{id}/cambiar-password")
    public ResponseEntity<Void> cambiarPassword(
            @PathVariable Long id,
            @RequestParam String nuevaPassword) {
        usuarioService.cambiarPassword(id, nuevaPassword);
        return ResponseEntity.noContent().build();
    }
}