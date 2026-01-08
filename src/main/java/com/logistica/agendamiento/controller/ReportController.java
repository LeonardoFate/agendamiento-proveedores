// src/main/java/com/logistica/agendamiento/controller/ReportController.java - CORREGIDO CON FORMATO DE TIEMPO
package com.logistica.agendamiento.controller;

import com.logistica.agendamiento.entity.*;
import com.logistica.agendamiento.entity.enums.EstadoReserva;
import com.logistica.agendamiento.entity.enums.TipoRegistro;
import com.logistica.agendamiento.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReservaRepository reservaRepository;
    private final AreaRepository areaRepository;
    private final AndenRepository andenRepository;
    private final ProveedorRepository proveedorRepository;
    private final RegistroTiempoRepository registroTiempoRepository;
    private final com.logistica.agendamiento.service.ExcelReportService excelReportService;
    private final com.logistica.agendamiento.service.PdfReportService pdfReportService;

    // ===== MÉTODO HELPER PARA FORMATEAR DURACIÓN =====
    private String formatDuration(double durationInSeconds) {
        // ✅ VALIDAR SI EL VALOR ES NaN, INFINITO O NEGATIVO
        if (Double.isNaN(durationInSeconds) || Double.isInfinite(durationInSeconds) || durationInSeconds <= 0) {
            return "Sin datos";
        }

        long totalSeconds = Math.round(durationInSeconds);

        // ✅ VALIDACIÓN ADICIONAL DESPUÉS DEL REDONDEO
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

        if (seconds > 0 && hours == 0) { // Solo mostrar segundos si no hay horas
            formatted.append(seconds).append("s");
        }

        // Si solo hay minutos sin segundos adicionales
        String result = formatted.toString().trim();
        if (result.isEmpty()) {
            return "< 1s";
        }

        return result;
    }

    // ===== ESTADÍSTICAS DE RESERVAS POR FECHA =====
    @GetMapping("/reservations")
    public ResponseEntity<List<Map<String, Object>>> getReservationStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("📊 Obteniendo estadísticas de reservas del {} al {}", startDate, endDate);

        List<Reserva> reservas = reservaRepository.findAll().stream()
                .filter(r -> !r.getFecha().isBefore(startDate) && !r.getFecha().isAfter(endDate))
                .collect(Collectors.toList());

        // Agrupar por fecha
        Map<LocalDate, Map<String, Long>> statsByDate = reservas.stream()
                .collect(Collectors.groupingBy(
                        Reserva::getFecha,
                        Collectors.groupingBy(
                                r -> r.getEstado().name(),
                                Collectors.counting()
                        )
                ));

        List<Map<String, Object>> result = new ArrayList<>();

        // Incluir todas las fechas del rango, incluso si no tienen reservas
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Map<String, Long> dayStats = statsByDate.getOrDefault(date, new HashMap<>());

            long total = dayStats.values().stream().mapToLong(Long::longValue).sum();
            long completed = dayStats.getOrDefault("COMPLETADA", 0L);
            long canceled = dayStats.getOrDefault("CANCELADA", 0L);
            long pending = dayStats.getOrDefault("PENDIENTE_CONFIRMACION", 0L) +
                    dayStats.getOrDefault("CONFIRMADA", 0L) +
                    dayStats.getOrDefault("EN_PLANTA", 0L) +
                    dayStats.getOrDefault("EN_RECEPCION", 0L);

            Map<String, Object> dayStat = new HashMap<>();
            dayStat.put("date", date.toString());
            dayStat.put("total", total);
            dayStat.put("completed", completed);
            dayStat.put("canceled", canceled);
            dayStat.put("pending", pending);

            result.add(dayStat);
        }

        log.info("✅ Estadísticas de reservas generadas para {} días", result.size());
        return ResponseEntity.ok(result);
    }

    // ===== ESTADÍSTICAS POR ÁREA =====
    @GetMapping("/areas")
    public ResponseEntity<List<Map<String, Object>>> getAreaStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("📊 Obteniendo estadísticas por área del {} al {}", startDate, endDate);

        List<Reserva> reservas = reservaRepository.findAll().stream()
                .filter(r -> !r.getFecha().isBefore(startDate) && !r.getFecha().isAfter(endDate))
                .filter(r -> r.getArea() != null) // Solo reservas con área asignada
                .collect(Collectors.toList());

        long totalReservas = reservas.size();

        Map<String, Long> areaCount = reservas.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getArea().getNombre(),
                        Collectors.counting()
                ));

        List<Map<String, Object>> result = areaCount.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> areaStat = new HashMap<>();
                    areaStat.put("areaName", entry.getKey());
                    areaStat.put("total", entry.getValue());
                    areaStat.put("percentage", totalReservas > 0 ?
                            (entry.getValue().doubleValue() / totalReservas) * 100 : 0.0);
                    return areaStat;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("total"), (Long) a.get("total")))
                .collect(Collectors.toList());

        log.info("✅ Estadísticas por área generadas para {} áreas", result.size());
        return ResponseEntity.ok(result);
    }

    // ===== ESTADÍSTICAS DE EFICIENCIA DE ANDENES (CORREGIDA CON FORMATO) =====
    @GetMapping("/efficiency")
    public ResponseEntity<List<Map<String, Object>>> getEfficiencyStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("📊 Obteniendo estadísticas de eficiencia del {} al {}", startDate, endDate);

        List<Reserva> reservas = reservaRepository.findAll().stream()
                .filter(r -> !r.getFecha().isBefore(startDate) && !r.getFecha().isAfter(endDate))
                .filter(r -> r.getAnden() != null) // Solo reservas con andén asignado
                .collect(Collectors.toList());

        // ✅ OBTENER TODOS LOS REGISTROS DE TIEMPO DEL PERÍODO
        List<RegistroTiempo> registrosTiempo = registroTiempoRepository.findAll().stream()
                .filter(rt -> {
                    LocalDate fechaRegistro = rt.getHoraInicio().toLocalDate();
                    return !fechaRegistro.isBefore(startDate) && !fechaRegistro.isAfter(endDate);
                })
                .filter(rt -> rt.getDuracion() != null && rt.getDuracion() > 0) // Solo registros finalizados
                .collect(Collectors.toList());

        log.info("🔍 Registros de tiempo encontrados: {}", registrosTiempo.size());

        // Agrupar por andén
        Map<Anden, List<Reserva>> reservasPorAnden = reservas.stream()
                .collect(Collectors.groupingBy(Reserva::getAnden));

        List<Map<String, Object>> result = reservasPorAnden.entrySet().stream()
                .map(entry -> {
                    Anden anden = entry.getKey();
                    List<Reserva> reservasAnden = entry.getValue();

                    // ✅ CALCULAR TIEMPO REAL BASADO EN REGISTROS DE TIEMPO
                    List<Long> reservaIds = reservasAnden.stream()
                            .map(Reserva::getId)
                            .collect(Collectors.toList());

                    // Obtener registros de tiempo para estas reservas
                    List<RegistroTiempo> registrosAnden = registrosTiempo.stream()
                            .filter(rt -> reservaIds.contains(rt.getReserva().getId()))
                            .collect(Collectors.toList());

                    double tiempoPromedioSegundos = 0.0;

                    if (!registrosAnden.isEmpty()) {
                        // ✅ USAR LA DURACIÓN REAL DE LOS REGISTROS (en segundos)
                        tiempoPromedioSegundos = registrosAnden.stream()
                                .filter(rt -> rt.getDuracion() != null && rt.getDuracion() > 0) // ✅ FILTRAR NULOS
                                .mapToDouble(RegistroTiempo::getDuracion)
                                .average()
                                .orElse(0.0);

                        log.debug("🕐 Andén {}: {} registros válidos, promedio {} segundos",
                                anden.getNumero(), registrosAnden.size(), tiempoPromedioSegundos);
                    } else {
                        // ✅ FALLBACK: Si no hay registros, usar diferencia de horarios programados
                        tiempoPromedioSegundos = reservasAnden.stream()
                                .filter(r -> r.getHoraInicio() != null && r.getHoraFin() != null)
                                .mapToDouble(r -> {
                                    LocalTime inicio = r.getHoraInicio();
                                    LocalTime fin = r.getHoraFin();
                                    long duracionSegundos = java.time.Duration.between(inicio, fin).toSeconds();
                                    return duracionSegundos > 0 ? duracionSegundos : 0; // ✅ VALIDAR POSITIVO
                                })
                                .filter(duracion -> duracion > 0) // ✅ FILTRAR DURACIONES INVÁLIDAS
                                .average()
                                .orElse(0.0);

                        log.warn("⚠️ Andén {}: Sin registros de tiempo, usando horarios programados: {} segundos",
                                anden.getNumero(), tiempoPromedioSegundos);
                    }

                    // ✅ VALIDACIÓN FINAL ANTES DE FORMATEAR
                    if (Double.isNaN(tiempoPromedioSegundos) || tiempoPromedioSegundos <= 0) {
                        log.warn("⚠️ Andén {}: Tiempo promedio inválido, estableciendo a 0", anden.getNumero());
                        tiempoPromedioSegundos = 0.0;
                    }

                    // Calcular utilización (asumiendo un máximo de días en el período)
                    long diasEnPeriodo = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
                    double utilizacion = Math.min(100.0, (reservasAnden.size() / (double) diasEnPeriodo) * 100);

                    Map<String, Object> efficiency = new HashMap<>();
                    efficiency.put("andenNumber", anden.getNumero());
                    efficiency.put("areaName", anden.getArea().getNombre());
                    efficiency.put("utilizationPercentage", utilizacion);
                    efficiency.put("averageTimePerReservation", formatDuration(tiempoPromedioSegundos)); // ✅ Formato legible
                    efficiency.put("averageTimeInSeconds", Math.round(tiempoPromedioSegundos)); // Para cálculos adicionales
                    efficiency.put("reservationsCount", reservasAnden.size());

                    return efficiency;
                })
                .sorted((a, b) -> Double.compare(
                        (Double) b.get("utilizationPercentage"),
                        (Double) a.get("utilizationPercentage")
                ))
                .collect(Collectors.toList());

        log.info("✅ Estadísticas de eficiencia generadas para {} andenes", result.size());
        return ResponseEntity.ok(result);
    }

    // ===== ESTADÍSTICAS DE PROVEEDORES (CORREGIDA CON FORMATO) =====
    @GetMapping("/providers")
    public ResponseEntity<List<Map<String, Object>>> getProviderStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("📊 Obteniendo estadísticas de proveedores del {} al {}", startDate, endDate);

        List<Reserva> reservas = reservaRepository.findAll().stream()
                .filter(r -> !r.getFecha().isBefore(startDate) && !r.getFecha().isAfter(endDate))
                .collect(Collectors.toList());

        // ✅ OBTENER REGISTROS DE TIEMPO
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

        List<Map<String, Object>> result = reservasPorProveedor.entrySet().stream()
                .map(entry -> {
                    Proveedor proveedor = entry.getKey();
                    List<Reserva> reservasProveedor = entry.getValue();

                    long completadas = reservasProveedor.stream()
                            .mapToLong(r -> r.getEstado() == EstadoReserva.COMPLETADA ? 1 : 0)
                            .sum();

                    double porcentajeCompletadas = reservasProveedor.size() > 0 ?
                            (completadas / (double) reservasProveedor.size()) * 100 : 0.0;

                    // ✅ CALCULAR TIEMPO REAL BASADO EN REGISTROS
                    List<Long> reservaIds = reservasProveedor.stream()
                            .map(Reserva::getId)
                            .collect(Collectors.toList());

                    List<RegistroTiempo> registrosProveedor = registrosTiempo.stream()
                            .filter(rt -> reservaIds.contains(rt.getReserva().getId()))
                            .collect(Collectors.toList());

                    double tiempoPromedioSegundos = 0.0;

                    if (!registrosProveedor.isEmpty()) {
                        // Usar duración real de registros (mantener en segundos)
                        tiempoPromedioSegundos = registrosProveedor.stream()
                                .filter(rt -> rt.getDuracion() != null && rt.getDuracion() > 0) // ✅ FILTRAR NULOS
                                .mapToDouble(RegistroTiempo::getDuracion)
                                .average()
                                .orElse(0.0);
                    } else {
                        // ✅ SOLO CALCULAR TIEMPO PARA RESERVAS COMPLETADAS
                        tiempoPromedioSegundos = reservasProveedor.stream()
                                .filter(r -> r.getEstado() == EstadoReserva.COMPLETADA) // ✅ SOLO COMPLETADAS
                                .filter(r -> r.getHoraInicio() != null && r.getHoraFin() != null)
                                .mapToDouble(r -> {
                                    LocalTime inicio = r.getHoraInicio();
                                    LocalTime fin = r.getHoraFin();
                                    long duracionSegundos = java.time.Duration.between(inicio, fin).toSeconds();
                                    return duracionSegundos > 0 ? duracionSegundos : 0; // ✅ VALIDAR POSITIVO
                                })
                                .filter(duracion -> duracion > 0) // ✅ FILTRAR DURACIONES INVÁLIDAS
                                .average()
                                .orElse(0.0);
                    }

                    // ✅ VALIDACIÓN FINAL ANTES DE FORMATEAR
                    if (Double.isNaN(tiempoPromedioSegundos) || tiempoPromedioSegundos <= 0) {
                        log.warn("⚠️ Proveedor {}: Tiempo promedio inválido, estableciendo a 0", proveedor.getNombre());
                        tiempoPromedioSegundos = 0.0;
                    }

                    Map<String, Object> providerStat = new HashMap<>();
                    providerStat.put("providerName", proveedor.getNombre());
                    providerStat.put("reservationsCount", reservasProveedor.size());
                    providerStat.put("completedPercentage", porcentajeCompletadas);
                    providerStat.put("averageTime", formatDuration(tiempoPromedioSegundos)); // ✅ Formato legible
                    providerStat.put("averageTimeInSeconds", Math.round(tiempoPromedioSegundos)); // Para cálculos

                    return providerStat;
                })
                .sorted((a, b) -> Integer.compare(
                        (Integer) b.get("reservationsCount"),
                        (Integer) a.get("reservationsCount")
                ))
                .collect(Collectors.toList());

        log.info("✅ Estadísticas de proveedores generadas para {} proveedores", result.size());
        return ResponseEntity.ok(result);
    }

    // ===== RESUMEN GENERAL =====
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getReportSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("📊 Obteniendo resumen general del {} al {}", startDate, endDate);

        List<Reserva> reservas = reservaRepository.findAll().stream()
                .filter(r -> !r.getFecha().isBefore(startDate) && !r.getFecha().isAfter(endDate))
                .collect(Collectors.toList());

        long totalReservas = reservas.size();
        long completadas = reservas.stream().mapToLong(r -> r.getEstado() == EstadoReserva.COMPLETADA ? 1 : 0).sum();
        long canceladas = reservas.stream().mapToLong(r -> r.getEstado() == EstadoReserva.CANCELADA ? 1 : 0).sum();
        long pendientes = totalReservas - completadas - canceladas;

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalReservations", totalReservas);
        summary.put("completedReservations", completadas);
        summary.put("canceledReservations", canceladas);
        summary.put("pendingReservations", pendientes);
        summary.put("completionRate", totalReservas > 0 ? (completadas / (double) totalReservas) * 100 : 0.0);
        summary.put("cancellationRate", totalReservas > 0 ? (canceladas / (double) totalReservas) * 100 : 0.0);
        summary.put("period", Map.of("startDate", startDate, "endDate", endDate));

        log.info("✅ Resumen general generado: {} reservas totales", totalReservas);
        return ResponseEntity.ok(summary);
    }

    // ===== EXPORTACIÓN PDF CON FORMATO DE PLANTILLA =====
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportarPDF(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String reportType) {

        log.info("📥 Exportando reporte PDF con formato plantilla - Tipo: {}, Período: {} - {}",
                reportType, startDate, endDate);

        try {
            // Usar el nuevo servicio de reportes PDF con formato de plantilla (19 columnas)
            byte[] pdfBytes = pdfReportService.generarReportePdf(startDate, endDate, reportType);

            String filename = String.format("reporte-%s-%s-al-%s.pdf",
                    reportType, startDate, endDate);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);

        } catch (Exception e) {
            log.error("❌ Error generando PDF", e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== EXPORTACIÓN EXCEL CON FORMATO DE PLANTILLA =====
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String reportType) {

        log.info("📥 Exportando reporte Excel con formato plantilla - Tipo: {}, Período: {} - {}",
                reportType, startDate, endDate);

        try {
            // Usar el nuevo servicio de reportes Excel con formato de plantilla
            byte[] excelBytes = excelReportService.generarReporteExcel(startDate, endDate, reportType);

            String filename = String.format("reporte-%s-%s-al-%s.xlsx",
                    reportType, startDate, endDate);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(excelBytes, headers, org.springframework.http.HttpStatus.OK);

        } catch (Exception e) {
            log.error("❌ Error generando Excel", e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

// ===== NOTA: MÉTODOS DE GENERACIÓN MOVIDOS A SERVICIOS DEDICADOS =====
    // - Los métodos de generación de Excel están en ExcelReportServiceImpl
    // - Los métodos de generación de PDF están en PdfReportServiceImpl
    // Esto mantiene una mejor separación de responsabilidades y reutilización de código
}