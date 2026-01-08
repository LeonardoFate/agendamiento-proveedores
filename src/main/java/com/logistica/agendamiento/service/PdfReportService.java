package com.logistica.agendamiento.service;

import java.time.LocalDate;

/**
 * Servicio para generar reportes en formato PDF con formato de plantilla
 */
public interface PdfReportService {

    /**
     * Genera un reporte PDF basado en el tipo y rango de fechas
     */
    byte[] generarReportePdf(LocalDate startDate, LocalDate endDate, String reportType);

    /**
     * Genera reporte de reservas con formato completo (19 columnas)
     */
    byte[] generarReporteReservasPlantilla(LocalDate startDate, LocalDate endDate);

    /**
     * Genera reporte de eficiencia en PDF
     */
    byte[] generarReporteEficienciaPlantilla(LocalDate startDate, LocalDate endDate);

    /**
     * Genera reporte de proveedores en PDF
     */
    byte[] generarReporteProveedoresPlantilla(LocalDate startDate, LocalDate endDate);
}
