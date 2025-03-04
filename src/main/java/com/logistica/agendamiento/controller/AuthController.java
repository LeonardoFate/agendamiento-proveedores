package com.logistica.agendamiento.controller;

import com.logistica.agendamiento.dto.LoginRequest;
import com.logistica.agendamiento.dto.LoginResponse;
import com.logistica.agendamiento.dto.RegistroProveedorRequest;
import com.logistica.agendamiento.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/registro-proveedor")
    public ResponseEntity<String> registrarProveedor(@Valid @RequestBody RegistroProveedorRequest request) {
        authService.registrarProveedor(request);
        return ResponseEntity.ok("Proveedor registrado exitosamente");
    }
}