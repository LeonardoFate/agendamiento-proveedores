package com.logistica.agendamiento.service;

import com.logistica.agendamiento.dto.TipoServicioDTO;

import java.util.List;

public interface TipoServicioService {

    List<TipoServicioDTO> obtenerTodosTiposServicio();

    TipoServicioDTO obtenerTipoServicioPorId(Long id);

    TipoServicioDTO crearTipoServicio(TipoServicioDTO tipoServicioDTO);

    TipoServicioDTO actualizarTipoServicio(Long id, TipoServicioDTO tipoServicioDTO);

    void eliminarTipoServicio(Long id);
}