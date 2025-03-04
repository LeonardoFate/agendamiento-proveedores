package com.logistica.agendamiento.service;

import com.logistica.agendamiento.dto.ProveedorDTO;

import java.util.List;

public interface ProveedorService {

    List<ProveedorDTO> obtenerTodosLosProveedores();

    ProveedorDTO obtenerProveedorPorId(Long id);

    ProveedorDTO obtenerProveedorPorRuc(String ruc);

    ProveedorDTO actualizarProveedor(Long id, ProveedorDTO proveedorDTO);

    ProveedorDTO cambiarEstadoProveedor(Long id, Boolean estado);
}