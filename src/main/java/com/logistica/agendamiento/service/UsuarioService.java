package com.logistica.agendamiento.service;

import com.logistica.agendamiento.dto.UsuarioDTO;
import com.logistica.agendamiento.entity.enums.Rol;

import java.util.List;

public interface UsuarioService {

    List<UsuarioDTO> obtenerTodosLosUsuarios();

    UsuarioDTO obtenerUsuarioPorId(Long id);

    List<UsuarioDTO> obtenerUsuariosPorRol(Rol rol);

    UsuarioDTO crearUsuario(UsuarioDTO usuarioDTO);

    UsuarioDTO actualizarUsuario(Long id, UsuarioDTO usuarioDTO);

    UsuarioDTO cambiarEstadoUsuario(Long id, Boolean estado);

    void cambiarPassword(Long id, String nuevaPassword);
}