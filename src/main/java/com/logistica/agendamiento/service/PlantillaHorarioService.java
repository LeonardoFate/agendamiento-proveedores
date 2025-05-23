package com.logistica.agendamiento.service;

import com.logistica.agendamiento.dto.HorarioProveedorDTO;
import com.logistica.agendamiento.dto.PlantillaHorarioDTO;
import com.logistica.agendamiento.entity.PlantillaHorario;
import com.logistica.agendamiento.entity.enums.DiaSemana;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface PlantillaHorarioService {

    // Carga masiva desde Excel
    List<PlantillaHorario> cargarDesdeExcel(MultipartFile archivo);

    // Obtener horario del proveedor para un día específico
    PlantillaHorario obtenerHorarioProveedor(Long proveedorId, DiaSemana dia);

    // Obtener horarios de la semana para un proveedor
    List<PlantillaHorario> obtenerHorariosSemana(Long proveedorId);

    // CRUD para administradores
    PlantillaHorario crearHorario(PlantillaHorarioDTO dto);
    PlantillaHorario actualizarHorario(Long id, PlantillaHorarioDTO dto);
    void eliminarHorario(Long id);

    // Vista de plantillas para admin
    List<PlantillaHorario> obtenerPlantillaPorDia(DiaSemana dia);
    List<PlantillaHorario> obtenerTodasLasPlantillas();

    // Métodos específicos para el controller
    HorarioProveedorDTO obtenerHorarioProveedorPorFecha(String username, LocalDate fecha);
    List<HorarioProveedorDTO> obtenerHorarioProveedorSemana(String username, LocalDate fechaInicio);

    // Generación automática de reservas
    void generarReservasDesdeePlantillas(LocalDate fecha);
    void generarReservasSemanaCompleta(LocalDate fechaInicio);

    // Conversiones
    List<PlantillaHorarioDTO> convertirADTOList(List<PlantillaHorario> plantillas);
    PlantillaHorarioDTO convertirADTO(PlantillaHorario plantilla);
}