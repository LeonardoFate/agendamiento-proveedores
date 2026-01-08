package com.logistica.agendamiento.service.impl;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.logistica.agendamiento.entity.*;
import com.logistica.agendamiento.entity.enums.TipoRegistro;
import com.logistica.agendamiento.repository.ReservaRepository;
import com.logistica.agendamiento.service.PdfReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfReportServiceImpl implements PdfReportService {

    private final ReservaRepository reservaRepository;

    private static final String[] COLUMNAS_PLANTILLA_RESERVAS = {
            "DIA", "PROVEEDOR", "# PERSONAS",
            "Fecha Confirm.", "Fecha Recep.",
            "HORA INICIO", "HORA FIN", "Tiempo Desc.",
            "Hora Ingreso", "Hora Salida",
            "Tiempo In. Real", "Tiempo Fin Real", "Tiempo Desc. Real",
            "Chofer", "Ayudante", "Placa", "Camión", "Modelo", "Observaciones"
    };

    @Override
    public byte[] generarReportePdf(LocalDate startDate, LocalDate endDate, String reportType) {
        log.info("Generando reporte PDF - Tipo: {}, Período: {} - {}", reportType, startDate, endDate);

        return switch (reportType.toLowerCase()) {
            case "reservations", "reservas" -> generarReporteReservasPlantilla(startDate, endDate);
            case "efficiency", "eficiencia" -> generarReporteEficienciaPlantilla(startDate, endDate);
            case "providers", "proveedores" -> generarReporteProveedoresPlantilla(startDate, endDate);
            default -> generarReporteReservasPlantilla(startDate, endDate);
        };
    }

    @Override
    public byte[] generarReporteReservasPlantilla(LocalDate startDate, LocalDate endDate) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Usar A3 en horizontal para acomodar las 19 columnas
            Document document = new Document(PageSize.A3.rotate(), 20, 20, 30, 30);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Título
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("Reporte de Reservas - Formato Completo", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // Período
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Paragraph period = new Paragraph(
                    "Período: " + startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                            " - " + endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    subtitleFont
            );
            period.setAlignment(Element.ALIGN_CENTER);
            period.setSpacingAfter(15);
            document.add(period);

            // Obtener reservas del período
            List<Reserva> reservas = reservaRepository.findAll().stream()
                    .filter(r -> !r.getFecha().isBefore(startDate) && !r.getFecha().isAfter(endDate))
                    .sorted((r1, r2) -> r1.getFecha().compareTo(r2.getFecha()))
                    .collect(Collectors.toList());

            log.info("Total de reservas encontradas: {}", reservas.size());

            // Crear tabla con 19 columnas
            PdfPTable table = new PdfPTable(19);
            table.setWidthPercentage(100);

            // Anchos relativos de columnas (total debe ser proporcional)
            float[] columnWidths = {
                    3f,  // DIA
                    5f,  // PROVEEDOR
                    3f,  // # PERSONAS
                    4f,  // Fecha Confirm.
                    4f,  // Fecha Recep.
                    3f,  // HORA INICIO
                    3f,  // HORA FIN
                    3f,  // Tiempo Desc.
                    3f,  // Hora Ingreso
                    3f,  // Hora Salida
                    3f,  // Tiempo In. Real
                    3f,  // Tiempo Fin Real
                    3f,  // Tiempo Desc. Real
                    4f,  // Chofer
                    4f,  // Ayudante
                    3f,  // Placa
                    3f,  // Camión
                    3f,  // Modelo
                    5f   // Observaciones
            };
            table.setWidths(columnWidths);

            // Agregar encabezados con estilo
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6);
            for (String columna : COLUMNAS_PLANTILLA_RESERVAS) {
                PdfPCell headerCell = new PdfPCell(new Phrase(columna, headerFont));
                headerCell.setBackgroundColor(new BaseColor(79, 129, 189));
                headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                headerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                headerCell.setPadding(3);
                table.addCell(headerCell);
            }

            // Agregar datos
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 5);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (Reserva reserva : reservas) {
                // 1. DIA
                table.addCell(createCell(reserva.getFecha().format(dateFormatter), cellFont));

                // 2. PROVEEDOR
                table.addCell(createCell(
                        reserva.getProveedor() != null ? reserva.getProveedor().getNombre() : "",
                        cellFont
                ));

                // 3. # PERSONAS POR PROVEEDOR
                // Nota: Este campo no existe en la entidad Reserva actual
                table.addCell(createCell("-", cellFont));

                // 4. Fecha confirmación (aproximada con updatedAt si está confirmada)
                String fechaConfirmacion = "-";
                if (reserva.getEstado() == com.logistica.agendamiento.entity.enums.EstadoReserva.CONFIRMADA ||
                    reserva.getEstado() == com.logistica.agendamiento.entity.enums.EstadoReserva.COMPLETADA) {
                    fechaConfirmacion = reserva.getUpdatedAt() != null ?
                            reserva.getUpdatedAt().format(dateTimeFormatter) : "-";
                }
                table.addCell(createCell(fechaConfirmacion, cellFont));

                // 5. Fecha recepción (aproximada con updatedAt si está en recepción/planta)
                String fechaRecepcion = "-";
                if (reserva.getEstado() == com.logistica.agendamiento.entity.enums.EstadoReserva.EN_RECEPCION ||
                    reserva.getEstado() == com.logistica.agendamiento.entity.enums.EstadoReserva.EN_PLANTA) {
                    fechaRecepcion = reserva.getUpdatedAt() != null ?
                            reserva.getUpdatedAt().format(dateTimeFormatter) : "-";
                }
                table.addCell(createCell(fechaRecepcion, cellFont));

                // 6. HORA INICIO
                table.addCell(createCell(
                        reserva.getHoraInicio() != null ? reserva.getHoraInicio().toString() : "",
                        cellFont
                ));

                // 7. HORA FIN
                table.addCell(createCell(
                        reserva.getHoraFin() != null ? reserva.getHoraFin().toString() : "",
                        cellFont
                ));

                // 8. Tiempo Descarga (estimado)
                String tiempoDescarga = "";
                if (reserva.getHoraInicio() != null && reserva.getHoraFin() != null) {
                    LocalDateTime inicio = LocalDateTime.of(reserva.getFecha(), reserva.getHoraInicio());
                    LocalDateTime fin = LocalDateTime.of(reserva.getFecha(), reserva.getHoraFin());
                    Duration duration = Duration.between(inicio, fin);
                    tiempoDescarga = formatearDuracion(duration);
                }
                table.addCell(createCell(tiempoDescarga, cellFont));

                // 9. Hora ingreso a planta (registro real)
                LocalDateTime horaIngresoPlanta = reserva.getRegistrosTiempo().stream()
                        .filter(rt -> rt.getTipo() == TipoRegistro.INGRESO_PLANTA)
                        .map(RegistroTiempo::getHoraInicio)
                        .findFirst()
                        .orElse(null);
                table.addCell(createCell(
                        horaIngresoPlanta != null ? horaIngresoPlanta.format(dateTimeFormatter) : "",
                        cellFont
                ));

                // 10. Hora salida de planta (registro real)
                LocalDateTime horaSalidaPlanta = reserva.getRegistrosTiempo().stream()
                        .filter(rt -> rt.getTipo() == TipoRegistro.SALIDA_PLANTA)
                        .map(RegistroTiempo::getHoraInicio)
                        .findFirst()
                        .orElse(null);
                table.addCell(createCell(
                        horaSalidaPlanta != null ? horaSalidaPlanta.format(dateTimeFormatter) : "",
                        cellFont
                ));

                // 11. Tiempo inicio real
                table.addCell(createCell(
                        horaIngresoPlanta != null ? horaIngresoPlanta.toLocalTime().toString() : "",
                        cellFont
                ));

                // 12. Tiempo fin real
                table.addCell(createCell(
                        horaSalidaPlanta != null ? horaSalidaPlanta.toLocalTime().toString() : "",
                        cellFont
                ));

                // 13. Tiempo descarga real
                String tiempoDescargaReal = "";
                if (horaIngresoPlanta != null && horaSalidaPlanta != null) {
                    Duration durationReal = Duration.between(horaIngresoPlanta, horaSalidaPlanta);
                    tiempoDescargaReal = formatearDuracion(durationReal);
                }
                table.addCell(createCell(tiempoDescargaReal, cellFont));

                // 14. Chofer
                String chofer = "";
                if (reserva.getTransporte() != null && reserva.getTransporte().getTransportistas() != null) {
                    chofer = reserva.getTransporte().getTransportistas().stream()
                            .filter(t -> t.getEsConductor() != null && t.getEsConductor())
                            .map(t -> t.getNombres() + " " + t.getApellidos())
                            .findFirst()
                            .orElse("");
                }
                table.addCell(createCell(chofer, cellFont));

                // 15. Ayudante
                String ayudante = "";
                if (reserva.getTransporte() != null && reserva.getTransporte().getTransportistas() != null) {
                    ayudante = reserva.getTransporte().getTransportistas().stream()
                            .filter(t -> t.getEsConductor() == null || !t.getEsConductor())
                            .map(t -> t.getNombres() + " " + t.getApellidos())
                            .findFirst()
                            .orElse("");
                }
                table.addCell(createCell(ayudante, cellFont));

                // 16. Placa
                table.addCell(createCell(
                        reserva.getTransporte() != null ? reserva.getTransporte().getPlaca() : "",
                        cellFont
                ));

                // 17. Camión (tipo de transporte)
                table.addCell(createCell(
                        reserva.getTransporte() != null ? reserva.getTransporte().getTipo() : "",
                        cellFont
                ));

                // 18. Modelo
                table.addCell(createCell(
                        reserva.getTransporte() != null ? reserva.getTransporte().getModelo() : "",
                        cellFont
                ));

                // 19. Observaciones
                String observaciones = reserva.getObservaciones().stream()
                        .map(Observacion::getDescripcion)
                        .collect(Collectors.joining("; "));
                table.addCell(createCell(observaciones, cellFont));
            }

            document.add(table);

            // Agregar nota al final
            Paragraph nota = new Paragraph(
                    "\nTotal de registros: " + reservas.size(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)
            );
            nota.setSpacingBefore(10);
            document.add(nota);

            document.close();
            log.info("PDF generado exitosamente con {} reservas", reservas.size());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generando PDF de reservas", e);
            throw new RuntimeException("Error generando PDF de reservas", e);
        }
    }

    @Override
    public byte[] generarReporteEficienciaPlantilla(LocalDate startDate, LocalDate endDate) {
        // Implementación simplificada - puedes expandirla según necesidades
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("Reporte de Eficiencia", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph message = new Paragraph("Reporte de eficiencia no implementado aún.",
                    FontFactory.getFont(FontFactory.HELVETICA, 12));
            message.setAlignment(Element.ALIGN_CENTER);
            message.setSpacingBefore(50);
            document.add(message);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de eficiencia", e);
        }
    }

    @Override
    public byte[] generarReporteProveedoresPlantilla(LocalDate startDate, LocalDate endDate) {
        // Implementación simplificada - puedes expandirla según necesidades
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("Reporte de Proveedores", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph message = new Paragraph("Reporte de proveedores no implementado aún.",
                    FontFactory.getFont(FontFactory.HELVETICA, 12));
            message.setAlignment(Element.ALIGN_CENTER);
            message.setSpacingBefore(50);
            document.add(message);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de proveedores", e);
        }
    }

    // ===== MÉTODOS AUXILIARES =====

    private PdfPCell createCell(String content, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(content != null ? content : "", font));
        cell.setPadding(2);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private String formatearDuracion(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return "";
        }

        long horas = duration.toHours();
        long minutos = duration.toMinutesPart();
        long segundos = duration.toSecondsPart();

        StringBuilder sb = new StringBuilder();
        if (horas > 0) {
            sb.append(horas).append("h ");
        }
        if (minutos > 0) {
            sb.append(minutos).append("m ");
        }
        if (segundos > 0 && horas == 0) {
            sb.append(segundos).append("s");
        }

        return sb.toString().trim();
    }
}
