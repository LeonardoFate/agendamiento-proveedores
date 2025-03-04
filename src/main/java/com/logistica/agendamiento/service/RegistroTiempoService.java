package com.logistica.agendamiento.service;

import com.logistica.agendamiento.dto.RegistroTiempoDTO;
import com.logistica.agendamiento.entity.enums.TipoRegistro;

import java.time.LocalDate;
import java.util.List;

public interface RegistroTiempoService {

    List<RegistroTiempoDTO> obtenerRegistrosPorReservaId(Long reservaId);

    RegistroTiempoDTO iniciarRegistro(Long reservaId, Long usuarioId, TipoRegistro tipo);

    RegistroTiempoDTO finalizarRegistro(Long registroId);

    List<RegistroTiempoDTO> obtenerRegistrosPorFecha(LocalDate fecha);

    Double obtenerTiempoPromedioByTipo(TipoRegistro tipo);
}