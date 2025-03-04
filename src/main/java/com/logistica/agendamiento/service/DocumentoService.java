package com.logistica.agendamiento.service;

import com.logistica.agendamiento.dto.DocumentoDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentoService {

    List<DocumentoDTO> obtenerDocumentosPorReservaId(Long reservaId);

    DocumentoDTO obtenerDocumentoPorId(Long id);

    DocumentoDTO guardarDocumento(Long reservaId, MultipartFile archivo, String descripcion);

    Resource descargarDocumento(Long id);

    void eliminarDocumento(Long id);
}