package com.logistica.agendamiento.service;

import com.logistica.agendamiento.dto.LoginRequest;
import com.logistica.agendamiento.dto.LoginResponse;
import com.logistica.agendamiento.dto.RegistroProveedorRequest;
import com.logistica.agendamiento.entity.Proveedor;
import com.logistica.agendamiento.entity.Usuario;
import com.logistica.agendamiento.entity.enums.Rol;
import com.logistica.agendamiento.exception.ResourceAlreadyExistsException;
import com.logistica.agendamiento.repository.ProveedorRepository;
import com.logistica.agendamiento.repository.UsuarioRepository;
import com.logistica.agendamiento.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final ProveedorRepository proveedorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwt = tokenProvider.generateToken(userDetails.getUsername(),
                userDetails.getAuthorities().iterator().next().getAuthority());

        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return new LoginResponse(
                jwt,
                "Bearer",
                usuario.getId(),
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getRol().name()
        );
    }

    @Transactional
    public void registrarProveedor(RegistroProveedorRequest request) {
        // Verificar si el username ya existe
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new ResourceAlreadyExistsException("El nombre de usuario ya está en uso");
        }

        // Verificar si el email ya existe
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("El email ya está en uso");
        }

        // Verificar si el RUC ya existe
        if (proveedorRepository.existsByRuc(request.getRuc())) {
            throw new ResourceAlreadyExistsException("El RUC ya está registrado");
        }

        // Crear el usuario
        Usuario usuario = new Usuario();
        usuario.setUsername(request.getUsername());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setEmail(request.getEmail());
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setRol(Rol.PROVEEDOR);
        usuario.setEstado(true);

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // Crear el proveedor
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(request.getNombreEmpresa());
        proveedor.setRuc(request.getRuc());
        proveedor.setDireccion(request.getDireccion());
        proveedor.setTelefono(request.getTelefono());
        proveedor.setEmail(request.getEmail());
        proveedor.setEstado(true);
        proveedor.setUsuario(usuarioGuardado);

        proveedorRepository.save(proveedor);
    }
}