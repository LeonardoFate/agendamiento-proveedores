package com.logistica.agendamiento.service.impl;

import com.logistica.agendamiento.entity.*;
import com.logistica.agendamiento.entity.enums.EstadoReserva;
import com.logistica.agendamiento.repository.*;
import com.logistica.agendamiento.service.ExcelReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelReportServiceImpl implements ExcelReportService {

    private final ReservaRepository reservaRepository;
    private final RegistroTiempoRepository registroTiempoRepository;

    private static final String[] COLUMNAS_PLANTILLA_RESERVAS = {
            "DIA", "PROVEEDOR", "# PERSONAS POR PROVEEDOR",
            "fecha confirmacion", "fecha recepcion",
            "HORA INICIO", "HORA FIN", "tiempo Descarga",
            "hora ingreso a planta", "hora salida de planta",
            "tiempo inicio real", "tiempo fin real", "tiempo descarga real",
            "chofer", "ayudante", "placa", "camion", "modelo", "observaciones"
    };

    private static final String[] COLUMNAS_PLANTILLA_EFICIENCIA = {
            "Andén", "Área", "Total Reservas", "Utilización %", "Tiempo Promedio"
    };

    private static final String[] COLUMNAS_PLANTILLA_PROVEEDORES = {
            "Proveedor", "Total Reservas", "% Completadas", "Tiempo Promedio"
    };

    @Override
    public byte[] generarReporteExcel(LocalDate startDate, LocalDate endDate, String reportType) {
        log.info("📊 Generando reporte Excel tipo '{}' del {} al {}", reportType, startDate, endDate);

        return switch (reportType.toLowerCase()) {
            case "reservations", "reservas" -> generarReporteReservasPlantilla(startDate, endDate);
            case "efficiency", "eficiencia" -> generarReporteEficienciaPlantilla(startDate, endDate);
            case "providers", "proveedores" -> generarReporteProveedoresPlantilla(startDate, endDate);
            default -> throw new IllegalArgumentException("Tipo de reporte no válido: " + reportType);
        };
    }

    @Override
    public byte[] generarReporteReservasPlantilla(LocalDate startDate, LocalDate endDate) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Reservas");

            // Crear estilos
            CellStyle headerStyle = crearEstiloEncabezado(workbook);
            CellStyle dataStyle = crearEstiloDatos(workbook);
            CellStyle timeStyle = crearEstiloHora(workbook);

            // Crear encabezados
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < COLUMNAS_PLANTILLA_RESERVAS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(COLUMNAS_PLANTILLA_RESERVAS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Obtener reservas del período
            List<Reserva> reservas = reservaRepository.findAll().stream()
                    .filter(r -> !r.getFecha().isBefore(startDate) && !r.getFecha().isAfter(endDate))
                    .sorted(Comparator.comparing(Reserva::getFecha)
                            .thenComparing(Reserva::getHoraInicio))
                    .collect(Collectors.toList());

            // Llenar datos
            int rowNum = 1;
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (Reserva reserva : reservas) {
                Row row = sheet.createRow(rowNum++);
                int colIndex = 0;

                // Obtener registros de tiempo para esta reserva
                List<RegistroTiempo> registros = reserva.getRegistrosTiempo();
                RegistroTiempo registroIngreso = registros.stream()
                        .filter(r -> r.getTipo() == com.logistica.agendamiento.entity.enums.TipoRegistro.INGRESO_PLANTA)
                        .findFirst()
                        .orElse(null);
                RegistroTiempo registroSalida = registros.stream()
                        .filter(r -> r.getTipo() == com.logistica.agendamiento.entity.enums.TipoRegistro.SALIDA_PLANTA)
                        .findFirst()
                        .orElse(null);

                // Obtener observaciones
                String observaciones = reserva.getObservaciones().stream()
                        .map(Observacion::getDescripcion)
                        .filter(desc -> desc != null && !desc.isEmpty())
                        .collect(Collectors.joining("; "));

                // DIA
                Cell cellDia = row.createCell(colIndex++);
                String diaSemana = reserva.getFecha()
                        .getDayOfWeek()
                        .getDisplayName(TextStyle.FULL, new Locale("es", "ES"))
                        .toUpperCase();
                cellDia.setCellValue(diaSemana);
                cellDia.setCellStyle(dataStyle);

                // PROVEEDOR
                Cell cellProveedor = row.createCell(colIndex++);
                cellProveedor.setCellValue(reserva.getProveedor().getNombre());
                cellProveedor.setCellStyle(dataStyle);

                // # PERSONAS POR PROVEEDOR
                Cell cellPersonas = row.createCell(colIndex++);
                cellPersonas.setCellValue("-");
                cellPersonas.setCellStyle(dataStyle);

                // fecha confirmacion (cuando pasó a CONFIRMADA)
                Cell cellFechaConfirmacion = row.createCell(colIndex++);
                cellFechaConfirmacion.setCellValue(reserva.getEstado() == EstadoReserva.CONFIRMADA ||
                                                   reserva.getEstado() == EstadoReserva.COMPLETADA ?
                                                   reserva.getUpdatedAt().format(dateTimeFormatter) : "-");
                cellFechaConfirmacion.setCellStyle(dataStyle);

                // fecha recepcion (cuando pasó a EN_RECEPCION o EN_PLANTA)
                Cell cellFechaRecepcion = row.createCell(colIndex++);
                cellFechaRecepcion.setCellValue(reserva.getEstado() == EstadoReserva.EN_PLANTA ||
                                                reserva.getEstado() == EstadoReserva.COMPLETADA ?
                                                reserva.getUpdatedAt().format(dateTimeFormatter) : "-");
                cellFechaRecepcion.setCellStyle(dataStyle);

                // HORA INICIO (programada)
                Cell cellHoraInicio = row.createCell(colIndex++);
                cellHoraInicio.setCellValue(reserva.getHoraInicio().format(timeFormatter));
                cellHoraInicio.setCellStyle(timeStyle);

                // HORA FIN (programada)
                Cell cellHoraFin = row.createCell(colIndex++);
                cellHoraFin.setCellValue(reserva.getHoraFin().format(timeFormatter));
                cellHoraFin.setCellStyle(timeStyle);

                // tiempo Descarga (duración estimada)
                Cell cellTiempoDescarga = row.createCell(colIndex++);
                LocalTime duracion = calcularDuracion(reserva.getHoraInicio(), reserva.getHoraFin());
                cellTiempoDescarga.setCellValue(duracion.format(timeFormatter));
                cellTiempoDescarga.setCellStyle(timeStyle);

                // hora ingreso a planta (real)
                Cell cellIngresoPlanta = row.createCell(colIndex++);
                cellIngresoPlanta.setCellValue(registroIngreso != null ?
                        registroIngreso.getHoraInicio().format(dateTimeFormatter) : "-");
                cellIngresoPlanta.setCellStyle(dataStyle);

                // hora salida de planta (real)
                Cell cellSalidaPlanta = row.createCell(colIndex++);
                cellSalidaPlanta.setCellValue(registroSalida != null && registroSalida.getHoraFin() != null ?
                        registroSalida.getHoraFin().format(dateTimeFormatter) : "-");
                cellSalidaPlanta.setCellStyle(dataStyle);

                // tiempo inicio real
                Cell cellTiempoInicioReal = row.createCell(colIndex++);
                cellTiempoInicioReal.setCellValue(registroIngreso != null ?
                        registroIngreso.getHoraInicio().toLocalTime().format(timeFormatter) : "-");
                cellTiempoInicioReal.setCellStyle(timeStyle);

                // tiempo fin real
                Cell cellTiempoFinReal = row.createCell(colIndex++);
                cellTiempoFinReal.setCellValue(registroSalida != null && registroSalida.getHoraFin() != null ?
                        registroSalida.getHoraFin().toLocalTime().format(timeFormatter) : "-");
                cellTiempoFinReal.setCellStyle(timeStyle);

                // tiempo descarga real (duración real si hay registros)
                Cell cellTiempoDescargaReal = row.createCell(colIndex++);
                if (registroIngreso != null && registroIngreso.getDuracion() != null) {
                    cellTiempoDescargaReal.setCellValue(formatDuration(registroIngreso.getDuracion()));
                } else {
                    cellTiempoDescargaReal.setCellValue("-");
                }
                cellTiempoDescargaReal.setCellStyle(dataStyle);

                // chofer
                Cell cellChofer = row.createCell(colIndex++);
                if (reserva.getTransporte() != null && !reserva.getTransporte().getTransportistas().isEmpty()) {
                    String chofer = reserva.getTransporte().getTransportistas().stream()
                            .filter(t -> t.getEsConductor() != null && t.getEsConductor())
                            .map(t -> t.getNombres() + " " + t.getApellidos())
                            .findFirst()
                            .orElse("-");
                    cellChofer.setCellValue(chofer);
                } else {
                    cellChofer.setCellValue("-");
                }
                cellChofer.setCellStyle(dataStyle);

                // ayudante
                Cell cellAyudante = row.createCell(colIndex++);
                if (reserva.getTransporte() != null && !reserva.getTransporte().getTransportistas().isEmpty()) {
                    String ayudante = reserva.getTransporte().getTransportistas().stream()
                            .filter(t -> t.getEsConductor() == null || !t.getEsConductor())
                            .map(t -> t.getNombres() + " " + t.getApellidos())
                            .collect(Collectors.joining(", "));
                    cellAyudante.setCellValue(ayudante.isEmpty() ? "-" : ayudante);
                } else {
                    cellAyudante.setCellValue("-");
                }
                cellAyudante.setCellStyle(dataStyle);

                // placa
                Cell cellPlaca = row.createCell(colIndex++);
                cellPlaca.setCellValue(reserva.getTransporte() != null ? reserva.getTransporte().getPlaca() : "-");
                cellPlaca.setCellStyle(dataStyle);

                // camion (tipo)
                Cell cellCamion = row.createCell(colIndex++);
                cellCamion.setCellValue(reserva.getTransporte() != null ? reserva.getTransporte().getTipo() : "-");
                cellCamion.setCellStyle(dataStyle);

                // modelo
                Cell cellModelo = row.createCell(colIndex++);
                cellModelo.setCellValue(reserva.getTransporte() != null ? reserva.getTransporte().getModelo() : "-");
                cellModelo.setCellStyle(dataStyle);

                // observaciones
                Cell cellObservaciones = row.createCell(colIndex++);
                cellObservaciones.setCellValue(observaciones.isEmpty() ? "-" : observaciones);
                cellObservaciones.setCellStyle(dataStyle);
            }

            // Ajustar ancho de columnas
            for (int i = 0; i < COLUMNAS_PLANTILLA_RESERVAS.length; i++) {
                sheet.autoSizeColumn(i);
                // Agregar un poco más de espacio
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
            }

            workbook.write(baos);
            log.info("✅ Reporte de reservas generado con {} registros", reservas.size());
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("❌ Error generando reporte Excel de reservas", e);
            throw new RuntimeException("Error al generar reporte Excel: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] generarReporteEficienciaPlantilla(LocalDate startDate, LocalDate endDate) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Eficiencia");

            // Crear estilos
            CellStyle headerStyle = crearEstiloEncabezado(workbook);
            CellStyle dataStyle = crearEstiloDatos(workbook);
            CellStyle percentStyle = crearEstiloPorcentaje(workbook);

            // Crear encabezados
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < COLUMNAS_PLANTILLA_EFICIENCIA.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(COLUMNAS_PLANTILLA_EFICIENCIA[i]);
                cell.setCellStyle(headerStyle);
            }

            // Obtener reservas del período
            List<Reserva> reservas = reservaRepository.findAll().stream()
                    .filter(r -> !r.getFecha().isBefore(startDate) && !r.getFecha().isAfter(endDate))
                    .filter(r -> r.getAnden() != null)
                    .collect(Collectors.toList());

            // Obtener registros de tiempo
            List<RegistroTiempo> registrosTiempo = registroTiempoRepository.findAll().stream()
                    .filter(rt -> {
                        LocalDate fechaRegistro = rt.getHoraInicio().toLocalDate();
                        return !fechaRegistro.isBefore(startDate) && !fechaRegistro.isAfter(endDate);
                    })
                    .filter(rt -> rt.getDuracion() != null && rt.getDuracion() > 0)
                    .collect(Collectors.toList());

            // Agrupar por andén
            Map<Anden, List<Reserva>> reservasPorAnden = reservas.stream()
                    .collect(Collectors.groupingBy(Reserva::getAnden));

            long diasEnPeriodo = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;

            // Llenar datos
            int rowNum = 1;
            List<Map.Entry<Anden, List<Reserva>>> sortedEntries = reservasPorAnden.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(a.getKey().getNumero(), b.getKey().getNumero()))
                    .collect(Collectors.toList());

            for (Map.Entry<Anden, List<Reserva>> entry : sortedEntries) {
                Anden anden = entry.getKey();
                List<Reserva> reservasAnden = entry.getValue();

                Row row = sheet.createRow(rowNum++);

                // Andén
                Cell cellAnden = row.createCell(0);
                cellAnden.setCellValue("Andén " + anden.getNumero());
                cellAnden.setCellStyle(dataStyle);

                // Área
                Cell cellArea = row.createCell(1);
                cellArea.setCellValue(anden.getArea().getNombre());
                cellArea.setCellStyle(dataStyle);

                // Total Reservas
                Cell cellTotal = row.createCell(2);
                cellTotal.setCellValue(reservasAnden.size());
                cellTotal.setCellStyle(dataStyle);

                // Utilización %
                Cell cellUtilizacion = row.createCell(3);
                double utilizacion = Math.min(100.0, (reservasAnden.size() / (double) diasEnPeriodo) * 100);
                cellUtilizacion.setCellValue(utilizacion / 100);
                cellUtilizacion.setCellStyle(percentStyle);

                // Tiempo Promedio
                Cell cellTiempoPromedio = row.createCell(4);
                double tiempoPromedioSegundos = calcularTiempoPromedioAnden(
                        reservasAnden, registrosTiempo
                );
                cellTiempoPromedio.setCellValue(formatDuration(tiempoPromedioSegundos));
                cellTiempoPromedio.setCellStyle(dataStyle);
            }

            // Ajustar ancho de columnas
            for (int i = 0; i < COLUMNAS_PLANTILLA_EFICIENCIA.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
            }

            workbook.write(baos);
            log.info("✅ Reporte de eficiencia generado con {} andenes", reservasPorAnden.size());
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("❌ Error generando reporte Excel de eficiencia", e);
            throw new RuntimeException("Error al generar reporte Excel: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] generarReporteProveedoresPlantilla(LocalDate startDate, LocalDate endDate) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Proveedores");

            // Crear estilos
            CellStyle headerStyle = crearEstiloEncabezado(workbook);
            CellStyle dataStyle = crearEstiloDatos(workbook);
            CellStyle percentStyle = crearEstiloPorcentaje(workbook);

            // Crear encabezados
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < COLUMNAS_PLANTILLA_PROVEEDORES.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(COLUMNAS_PLANTILLA_PROVEEDORES[i]);
                cell.setCellStyle(headerStyle);
            }

            // Obtener reservas del período
            List<Reserva> reservas = reservaRepository.findAll().stream()
                    .filter(r -> !r.getFecha().isBefore(startDate) && !r.getFecha().isAfter(endDate))
                    .collect(Collectors.toList());

            // Obtener registros de tiempo
            List<RegistroTiempo> registrosTiempo = registroTiempoRepository.findAll().stream()
                    .filter(rt -> {
                        LocalDate fechaRegistro = rt.getHoraInicio().toLocalDate();
                        return !fechaRegistro.isBefore(startDate) && !fechaRegistro.isAfter(endDate);
                    })
                    .filter(rt -> rt.getDuracion() != null && rt.getDuracion() > 0)
                    .collect(Collectors.toList());

            // Agrupar por proveedor
            Map<Proveedor, List<Reserva>> reservasPorProveedor = reservas.stream()
                    .collect(Collectors.groupingBy(Reserva::getProveedor));

            // Llenar datos
            int rowNum = 1;
            List<Map.Entry<Proveedor, List<Reserva>>> sortedEntries = reservasPorProveedor.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(
                            b.getValue().size(), a.getValue().size()
                    ))
                    .collect(Collectors.toList());

            for (Map.Entry<Proveedor, List<Reserva>> entry : sortedEntries) {
                Proveedor proveedor = entry.getKey();
                List<Reserva> reservasProveedor = entry.getValue();

                Row row = sheet.createRow(rowNum++);

                // Proveedor
                Cell cellProveedor = row.createCell(0);
                cellProveedor.setCellValue(proveedor.getNombre());
                cellProveedor.setCellStyle(dataStyle);

                // Total Reservas
                Cell cellTotal = row.createCell(1);
                cellTotal.setCellValue(reservasProveedor.size());
                cellTotal.setCellStyle(dataStyle);

                // % Completadas
                Cell cellCompletadas = row.createCell(2);
                long completadas = reservasProveedor.stream()
                        .filter(r -> r.getEstado() == EstadoReserva.COMPLETADA)
                        .count();
                double porcentajeCompletadas = reservasProveedor.size() > 0 ?
                        (completadas / (double) reservasProveedor.size()) : 0.0;
                cellCompletadas.setCellValue(porcentajeCompletadas);
                cellCompletadas.setCellStyle(percentStyle);

                // Tiempo Promedio
                Cell cellTiempoPromedio = row.createCell(3);
                double tiempoPromedioSegundos = calcularTiempoPromedioProveedor(
                        reservasProveedor, registrosTiempo
                );
                cellTiempoPromedio.setCellValue(formatDuration(tiempoPromedioSegundos));
                cellTiempoPromedio.setCellStyle(dataStyle);
            }

            // Ajustar ancho de columnas
            for (int i = 0; i < COLUMNAS_PLANTILLA_PROVEEDORES.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
            }

            workbook.write(baos);
            log.info("✅ Reporte de proveedores generado con {} proveedores", reservasPorProveedor.size());
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("❌ Error generando reporte Excel de proveedores", e);
            throw new RuntimeException("Error al generar reporte Excel: " + e.getMessage(), e);
        }
    }

    // ===== MÉTODOS DE ESTILOS =====

    private CellStyle crearEstiloEncabezado(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle crearEstiloDatos(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle crearEstiloHora(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle crearEstiloPorcentaje(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("0.0%"));
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    // ===== MÉTODOS AUXILIARES =====

    private LocalTime calcularDuracion(LocalTime inicio, LocalTime fin) {
        long segundos = java.time.Duration.between(inicio, fin).toSeconds();
        return LocalTime.ofSecondOfDay(segundos);
    }

    private double calcularTiempoPromedioAnden(List<Reserva> reservasAnden,
                                                List<RegistroTiempo> registrosTiempo) {
        List<Long> reservaIds = reservasAnden.stream()
                .map(Reserva::getId)
                .collect(Collectors.toList());

        List<RegistroTiempo> registrosAnden = registrosTiempo.stream()
                .filter(rt -> reservaIds.contains(rt.getReserva().getId()))
                .collect(Collectors.toList());

        if (!registrosAnden.isEmpty()) {
            return registrosAnden.stream()
                    .filter(rt -> rt.getDuracion() != null && rt.getDuracion() > 0)
                    .mapToDouble(RegistroTiempo::getDuracion)
                    .average()
                    .orElse(0.0);
        } else {
            // Fallback: usar horarios programados
            return reservasAnden.stream()
                    .filter(r -> r.getHoraInicio() != null && r.getHoraFin() != null)
                    .mapToDouble(r -> java.time.Duration.between(r.getHoraInicio(), r.getHoraFin()).toSeconds())
                    .filter(duracion -> duracion > 0)
                    .average()
                    .orElse(0.0);
        }
    }

    private double calcularTiempoPromedioProveedor(List<Reserva> reservasProveedor,
                                                    List<RegistroTiempo> registrosTiempo) {
        List<Long> reservaIds = reservasProveedor.stream()
                .map(Reserva::getId)
                .collect(Collectors.toList());

        List<RegistroTiempo> registrosProveedor = registrosTiempo.stream()
                .filter(rt -> reservaIds.contains(rt.getReserva().getId()))
                .collect(Collectors.toList());

        if (!registrosProveedor.isEmpty()) {
            return registrosProveedor.stream()
                    .filter(rt -> rt.getDuracion() != null && rt.getDuracion() > 0)
                    .mapToDouble(RegistroTiempo::getDuracion)
                    .average()
                    .orElse(0.0);
        } else {
            // Fallback: solo reservas completadas
            return reservasProveedor.stream()
                    .filter(r -> r.getEstado() == EstadoReserva.COMPLETADA)
                    .filter(r -> r.getHoraInicio() != null && r.getHoraFin() != null)
                    .mapToDouble(r -> java.time.Duration.between(r.getHoraInicio(), r.getHoraFin()).toSeconds())
                    .filter(duracion -> duracion > 0)
                    .average()
                    .orElse(0.0);
        }
    }

    private String formatDuration(double durationInSeconds) {
        if (Double.isNaN(durationInSeconds) || Double.isInfinite(durationInSeconds) || durationInSeconds <= 0) {
            return "Sin datos";
        }

        long totalSeconds = Math.round(durationInSeconds);

        if (totalSeconds <= 0) {
            return "0s";
        }

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder formatted = new StringBuilder();

        if (hours > 0) {
            formatted.append(hours).append("h ");
        }

        if (minutes > 0) {
            formatted.append(minutes).append("m ");
        }

        if (seconds > 0 && hours == 0) {
            formatted.append(seconds).append("s");
        }

        String result = formatted.toString().trim();
        return result.isEmpty() ? "< 1s" : result;
    }
}
