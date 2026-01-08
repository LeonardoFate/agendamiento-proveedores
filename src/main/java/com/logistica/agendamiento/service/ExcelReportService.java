package com.logistica.agendamiento.service;

import java.time.LocalDate;

public interface ExcelReportService {

    /**
     * Genera un reporte de reservas en formato Excel con la estructura de la plantilla
     * @param startDate Fecha de inicio del período
     * @param endDate Fecha de fin del período
     * @param reportType Tipo de reporte (reservations, efficiency, providers)
     * @return Archivo Excel en bytes
     */
    byte[] generarReporteExcel(LocalDate startDate, LocalDate endDate, String reportType);

    /**
     * Genera un reporte de reservas con la estructura de la plantilla original
     * Columnas: DIA | PROVEEDOR | # PERSONAS POR PROVEEDOR | HORA INICIO | HORA FIN | tiempo Descarga
     */
    byte[] generarReporteReservasPlantilla(LocalDate startDate, LocalDate endDate);

    /**
     * Genera un reporte de eficiencia de andenes con estilo de plantilla
     */
    byte[] generarReporteEficienciaPlantilla(LocalDate startDate, LocalDate endDate);

    /**
     * Genera un reporte de proveedores con estilo de plantilla
     */
    byte[] generarReporteProveedoresPlantilla(LocalDate startDate, LocalDate endDate);
}
