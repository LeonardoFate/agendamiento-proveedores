package com.logistica.agendamiento.service;

import com.logistica.agendamiento.dto.AreaDTO;
import java.util.List;

public interface AreaService {

    List<AreaDTO> obtenerTodasLasAreas();

    AreaDTO obtenerAreaPorId(Long id);

    AreaDTO crearArea(AreaDTO areaDTO);

    AreaDTO actualizarArea(Long id, AreaDTO areaDTO);

    void eliminarArea(Long id);
}