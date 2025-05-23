package com.logistica.agendamiento.service;

import com.logistica.agendamiento.dto.PlantillaHorarioDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ExcelProcessingService {

    List<PlantillaHorarioDTO> procesarExcelPlantilla(MultipartFile archivo);

    void validarFormatoExcel(MultipartFile archivo);

    PlantillaHorarioDTO mapearFilaExcel(org.apache.poi.ss.usermodel.Row fila);
}
