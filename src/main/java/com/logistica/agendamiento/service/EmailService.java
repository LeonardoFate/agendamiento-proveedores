package com.logistica.agendamiento.service;

import com.logistica.agendamiento.dto.ReservaDetalleDTO;

public interface EmailService {

    void enviarConfirmacionReserva(ReservaDetalleDTO reserva, String destinatario);

    void enviarNotificacionCambioEstado(ReservaDetalleDTO reserva, String destinatario);

    void enviarNotificacionCancelacion(ReservaDetalleDTO reserva, String destinatario);

    void enviarCorreo(String to, String subject, String content);
}