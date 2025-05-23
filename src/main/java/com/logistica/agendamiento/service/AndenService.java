package com.logistica.agendamiento.service;

import com.logistica.agendamiento.dto.AndenDTO;
import com.logistica.agendamiento.entity.enums.EstadoAnden;

import java.util.List;

public interface AndenService {

    List<AndenDTO> obtenerTodosLosAndenes();

    List<AndenDTO> obtenerAndenesPorArea(Long areaId);

    List<AndenDTO> obtenerAndenesPorAreaYEstado(Long areaId, EstadoAnden estado);

    AndenDTO obtenerAndenPorId(Long id);

    AndenDTO crearAnden(AndenDTO andenDTO);

    AndenDTO actualizarAnden(Long id, AndenDTO andenDTO);

    AndenDTO actualizarEstadoAnden(Long id, EstadoAnden estado);

    void eliminarAnden(Long id);
}