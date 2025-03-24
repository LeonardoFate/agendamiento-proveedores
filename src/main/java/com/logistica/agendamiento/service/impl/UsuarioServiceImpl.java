package com.logistica.agendamiento.service.impl;

import com.logistica.agendamiento.dto.UsuarioDTO;
import com.logistica.agendamiento.entity.Usuario;
import com.logistica.agendamiento.entity.enums.Rol;
import com.logistica.agendamiento.exception.ResourceAlreadyExistsException;
import com.logistica.agendamiento.exception.ResourceNotFoundException;
import com.logistica.agendamiento.repository.UsuarioRepository;
import com.logistica.agendamiento.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UsuarioDTO> obtenerTodosLosUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    public UsuarioDTO obtenerUsuarioPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        return convertirADTO(usuario);
    }

    @Override
    public List<UsuarioDTO> obtenerUsuariosPorRol(Rol rol) {
        return usuarioRepository.findByRol(rol).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    public UsuarioDTO crearUsuario(UsuarioDTO usuarioDTO) {
        // Verificar que el username no exista
        if (usuarioRepository.existsByUsername(usuarioDTO.getUsername())) {
            throw new ResourceAlreadyExistsException("El nombre de usuario ya está en uso: " + usuarioDTO.getUsername());
        }

        // Verificar que el email no exista
        if (usuarioRepository.existsByEmail(usuarioDTO.getEmail())) {
            throw new ResourceAlreadyExistsException("El email ya está en uso: " + usuarioDTO.getEmail());
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(usuarioDTO.getUsername());
        usuario.setPassword(passwordEncoder.encode(usuarioDTO.getPassword()));
        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setNombre(usuarioDTO.getNombre());
        usuario.setApellido(usuarioDTO.getApellido());
        usuario.setRol(usuarioDTO.getRol());
        usuario.setEstado(true);

        Usuario usuarioSaved = usuarioRepository.save(usuario);
        return convertirADTO(usuarioSaved);
    }

    @Override
    public UsuarioDTO actualizarUsuario(Long id, UsuarioDTO usuarioDTO) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        // Verificar que el username no esté en uso por otro usuario
        if (!usuario.getUsername().equals(usuarioDTO.getUsername()) &&
                usuarioRepository.existsByUsername(usuarioDTO.getUsername())) {
            throw new ResourceAlreadyExistsException("El nombre de usuario ya está en uso: " + usuarioDTO.getUsername());
        }

        // Verificar que el email no esté en uso por otro usuario
        if (!usuario.getEmail().equals(usuarioDTO.getEmail()) &&
                usuarioRepository.existsByEmail(usuarioDTO.getEmail())) {
            throw new ResourceAlreadyExistsException("El email ya está en uso: " + usuarioDTO.getEmail());
        }

        usuario.setUsername(usuarioDTO.getUsername());
        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setNombre(usuarioDTO.getNombre());
        usuario.setApellido(usuarioDTO.getApellido());
        usuario.setRol(usuarioDTO.getRol());

        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        return convertirADTO(usuarioActualizado);
    }

    @Override
    public UsuarioDTO cambiarEstadoUsuario(Long id, Boolean estado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        usuario.setEstado(estado);
        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        return convertirADTO(usuarioActualizado);
    }

    @Override
    public void cambiarPassword(Long id, String nuevaPassword) {
        log.info("Intentando cambiar contraseña para usuario ID: " + id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        log.info("Usuario encontrado: " + usuario.getUsername());
        String encodedPassword = passwordEncoder.encode(nuevaPassword);
        log.info("Nueva contraseña codificada");

        usuario.setPassword(encodedPassword);
        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        log.info("Usuario guardado. ¿Es la contraseña correcta?: " +
                passwordEncoder.matches(nuevaPassword, usuarioGuardado.getPassword()));
    }

    private UsuarioDTO convertirADTO(Usuario usuario) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(usuario.getId());
        dto.setUsername(usuario.getUsername());
        dto.setEmail(usuario.getEmail());
        dto.setNombre(usuario.getNombre());
        dto.setApellido(usuario.getApellido());
        dto.setRol(usuario.getRol());
        dto.setEstado(usuario.getEstado());
        return dto;
    }
}