package com.logistica.agendamiento.service.impl;

import com.logistica.agendamiento.dto.PlantillaHorarioDTO;
import com.logistica.agendamiento.entity.Proveedor;
import com.logistica.agendamiento.entity.enums.DiaSemana;
import com.logistica.agendamiento.exception.BadRequestException;
import com.logistica.agendamiento.exception.ResourceNotFoundException;
import com.logistica.agendamiento.repository.ProveedorRepository;
import com.logistica.agendamiento.service.ExcelProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelProcessingServiceImpl implements ExcelProcessingService {

    private final ProveedorRepository proveedorRepository;

    private static final String[] COLUMNAS_REQUERIDAS = {
            "DIA", "PROVEEDOR", "# PERSONAS POR PROVEEDOR",
            "HORA INICIO", "HORA FIN", "tiempo Descarga"
    };

    @Override
    public List<PlantillaHorarioDTO> procesarExcelPlantilla(MultipartFile archivo) {
        validarFormatoExcel(archivo);

        List<PlantillaHorarioDTO> plantillas = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(archivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Validar encabezados
            Row headerRow = sheet.getRow(0);
            validarEncabezados(headerRow);

            // Procesar cada fila
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null && !esFilaVacia(row)) {
                    try {
                        PlantillaHorarioDTO plantilla = mapearFilaExcel(row);
                        if (plantilla != null) {
                            plantillas.add(plantilla);
                        }
                    } catch (Exception e) {
                        log.error("Error procesando fila {}: {}", i + 1, e.getMessage());
                        throw new BadRequestException("Error en fila " + (i + 1) + ": " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            throw new BadRequestException("Error al leer el archivo Excel: " + e.getMessage());
        }

        if (plantillas.isEmpty()) {
            throw new BadRequestException("No se encontraron datos válidos en el archivo Excel");
        }

        log.info("Procesadas {} plantillas desde Excel", plantillas.size());
        return plantillas;
    }

    @Override
    public void validarFormatoExcel(MultipartFile archivo) {
        if (archivo.isEmpty()) {
            throw new BadRequestException("El archivo está vacío");
        }

        String fileName = archivo.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            throw new BadRequestException("El archivo debe ser de formato Excel (.xlsx o .xls)");
        }

        if (archivo.getSize() > 10 * 1024 * 1024) { // 10MB
            throw new BadRequestException("El archivo es demasiado grande. Máximo 10MB");
        }
    }

    @Override
    public PlantillaHorarioDTO mapearFilaExcel(Row fila) {
        PlantillaHorarioDTO plantilla = new PlantillaHorarioDTO();

        try {
            // DIA
            String diaStr = getCellValueAsString(fila.getCell(0));
            DiaSemana dia = convertirStringADiaSemana(diaStr);
            plantilla.setDia(dia);

            // PROVEEDOR
            String proveedorNombre = getCellValueAsString(fila.getCell(1));
            Proveedor proveedor = buscarProveedorPorNombre(proveedorNombre);
            plantilla.setProveedorId(proveedor.getId());
            plantilla.setProveedorNombre(proveedor.getNombre());

            // # PERSONAS POR PROVEEDOR
            double personasNum = getCellValueAsNumber(fila.getCell(2));
            plantilla.setNumeroPersonas((int) personasNum);

            // HORA INICIO
            String horaInicioStr = getCellValueAsString(fila.getCell(3));
            LocalTime horaInicio = parseTime(horaInicioStr);
            plantilla.setHoraInicio(horaInicio);

            // HORA FIN
            String horaFinStr = getCellValueAsString(fila.getCell(4));
            LocalTime horaFin = parseTime(horaFinStr);
            plantilla.setHoraFin(horaFin);

            // tiempo Descarga
            String tiempoDescargaStr = getCellValueAsString(fila.getCell(5));
            LocalTime tiempoDescarga = parseTime(tiempoDescargaStr);
            plantilla.setTiempoDescarga(tiempoDescarga);

            // Validaciones
            validarPlantilla(plantilla);

            return plantilla;

        } catch (Exception e) {
            log.error("Error mapeando fila: {}", e.getMessage());
            throw new BadRequestException("Error en la fila: " + e.getMessage());
        }
    }

    // MÉTODOS PRIVADOS

    private void validarEncabezados(Row headerRow) {
        if (headerRow == null) {
            throw new BadRequestException("El archivo no tiene encabezados");
        }

        for (int i = 0; i < COLUMNAS_REQUERIDAS.length; i++) {
            Cell cell = headerRow.getCell(i);
            String valor = getCellValueAsString(cell);

            if (!COLUMNAS_REQUERIDAS[i].equalsIgnoreCase(valor.trim())) {
                throw new BadRequestException("Columna " + (i + 1) + " debe ser '" +
                        COLUMNAS_REQUERIDAS[i] + "' pero encontró '" + valor + "'");
            }
        }
    }

    private boolean esFilaVacia(Row row) {
        for (int i = 0; i < COLUMNAS_REQUERIDAS.length; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && !getCellValueAsString(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalTime().toString();
                } else {
                    yield String.valueOf((int) cell.getNumericCellValue());
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private double getCellValueAsNumber(Cell cell) {
        if (cell == null) {
            throw new BadRequestException("Celda numérica requerida está vacía");
        }

        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Double.parseDouble(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    throw new BadRequestException("Valor no numérico: " + cell.getStringCellValue());
                }
            }
            default -> throw new BadRequestException("Tipo de celda no válido para número");
        };
    }

    private DiaSemana convertirStringADiaSemana(String diaStr) {
        if (diaStr == null || diaStr.trim().isEmpty()) {
            throw new BadRequestException("Día no puede estar vacío");
        }

        return switch (diaStr.trim().toUpperCase()) {
            case "LUNES" -> DiaSemana.LUNES;
            case "MARTES" -> DiaSemana.MARTES;
            case "MIERCOLES", "MIÉRCOLES" -> DiaSemana.MIERCOLES;
            case "JUEVES" -> DiaSemana.JUEVES;
            case "VIERNES" -> DiaSemana.VIERNES;
            case "SABADO", "SÁBADO" -> DiaSemana.SABADO;
            case "DOMINGO" -> DiaSemana.DOMINGO;
            default -> throw new BadRequestException("Día no válido: " + diaStr);
        };
    }

    private Proveedor buscarProveedorPorNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new BadRequestException("Nombre de proveedor no puede estar vacío");
        }

        return proveedorRepository.findByNombre(nombre.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + nombre));
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            throw new BadRequestException("Hora no puede estar vacía");
        }

        try {
            // Formatos posibles: HH:mm:ss, HH:mm, H:mm
            if (timeStr.length() == 8) { // HH:mm:ss
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm:ss"));
            } else if (timeStr.length() == 5) { // HH:mm
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
            } else if (timeStr.length() == 4) { // H:mm
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("H:mm"));
            } else {
                return LocalTime.parse(timeStr);
            }
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Formato de hora inválido: " + timeStr + ". Use formato HH:mm");
        }
    }

    private void validarPlantilla(PlantillaHorarioDTO plantilla) {
        // Validar horarios
        if (plantilla.getHoraInicio().isAfter(plantilla.getHoraFin())) {
            throw new BadRequestException("Hora de inicio debe ser antes que hora de fin");
        }

        // Validar número de personas
        if (plantilla.getNumeroPersonas() <= 0) {
            throw new BadRequestException("Número de personas debe ser mayor a 0");
        }

        // Validar tiempo de descarga
        if (plantilla.getTiempoDescarga().equals(LocalTime.MIN)) {
            throw new BadRequestException("Tiempo de descarga debe ser mayor a 0");
        }
    }
}