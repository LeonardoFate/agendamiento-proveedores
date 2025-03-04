package com.logistica.agendamiento.service.impl;

import com.logistica.agendamiento.dto.ReservaDetalleDTO;
import com.logistica.agendamiento.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender emailSender;
    private final TemplateEngine templateEngine;

    private static final String ADMIN_EMAIL = "admin@sistema.com";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    @Async
    public void enviarConfirmacionReserva(ReservaDetalleDTO reserva, String destinatario) {
        try {
            Context context = new Context();
            context.setVariable("reserva", reserva);
            context.setVariable("fecha", reserva.getFecha().format(DATE_FORMATTER));
            context.setVariable("horaInicio", reserva.getHoraInicio().format(TIME_FORMATTER));
            context.setVariable("horaFin", reserva.getHoraFin().format(TIME_FORMATTER));

            String subject = "Confirmación de Reserva #" + reserva.getId();
            String content = templateEngine.process("templates/email/confirmacion-reserva", context);

            enviarCorreoHTML(destinatario, subject, content);

            // Enviar copia al administrador
            enviarCorreoHTML(ADMIN_EMAIL, "Nueva Reserva #" + reserva.getId(), content);

        } catch (Exception e) {
            log.error("Error al enviar correo de confirmación de reserva", e);
        }
    }

    @Override
    @Async
    public void enviarNotificacionCambioEstado(ReservaDetalleDTO reserva, String destinatario) {
        try {
            Context context = new Context();
            context.setVariable("reserva", reserva);
            context.setVariable("fecha", reserva.getFecha().format(DATE_FORMATTER));
            context.setVariable("horaInicio", reserva.getHoraInicio().format(TIME_FORMATTER));
            context.setVariable("horaFin", reserva.getHoraFin().format(TIME_FORMATTER));

            String subject = "Actualización de Estado - Reserva #" + reserva.getId();
            String content = templateEngine.process("templates/email/cambio-estado", context);

            enviarCorreoHTML(destinatario, subject, content);

        } catch (Exception e) {
            log.error("Error al enviar correo de cambio de estado", e);
        }
    }

    @Override
    @Async
    public void enviarNotificacionCancelacion(ReservaDetalleDTO reserva, String destinatario) {
        try {
            Context context = new Context();
            context.setVariable("reserva", reserva);
            context.setVariable("fecha", reserva.getFecha().format(DATE_FORMATTER));
            context.setVariable("horaInicio", reserva.getHoraInicio().format(TIME_FORMATTER));
            context.setVariable("horaFin", reserva.getHoraFin().format(TIME_FORMATTER));

            String subject = "Cancelación de Reserva #" + reserva.getId();
            String content = templateEngine.process("templates/email/cancelacion-reserva", context);

            enviarCorreoHTML(destinatario, subject, content);

            // Enviar copia al administrador
            enviarCorreoHTML(ADMIN_EMAIL, "Reserva Cancelada #" + reserva.getId(), content);

        } catch (Exception e) {
            log.error("Error al enviar correo de cancelación de reserva", e);
        }
    }

    @Override
    @Async
    public void enviarCorreo(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            emailSender.send(message);
        } catch (Exception e) {
            log.error("Error al enviar correo", e);
        }
    }

    private void enviarCorreoHTML(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        emailSender.send(message);
    }
}