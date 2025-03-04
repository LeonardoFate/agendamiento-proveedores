package com.logistica.agendamiento.service.impl;

import com.logistica.agendamiento.dto.DocumentoDTO;
import com.logistica.agendamiento.entity.Documento;
import com.logistica.agendamiento.entity.Reserva;
import com.logistica.agendamiento.exception.ResourceNotFoundException;
import com.logistica.agendamiento.repository.DocumentoRepository;
import com.logistica.agendamiento.repository.ReservaRepository;
import com.logistica.agendamiento.service.DocumentoService;
import com.logistica.agendamiento.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentoServiceImpl implements DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final ReservaRepository reservaRepository;
    private final FileStorageService fileStorageService;

    @Override
    public List<DocumentoDTO> obtenerDocumentosPorReservaId(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con ID: " + reservaId));

        return documentoRepository.findByReserva(reserva).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    public DocumentoDTO obtenerDocumentoPorId(Long id) {
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado con ID: " + id));

        return convertirADTO(documento);
    }

    @Override
    @Transactional
    public DocumentoDTO guardarDocumento(Long reservaId, MultipartFile archivo, String descripcion) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con ID: " + reservaId));

        // Almacenar el archivo y obtener la ruta
        String rutaArchivo = fileStorageService.store(archivo, reservaId);

        // Crear el registro de documento
        Documento documento = new Documento();
        documento.setReserva(reserva);
        documento.setNombre(archivo.getOriginalFilename());
        documento.setRuta(rutaArchivo);
        documento.setTipo(archivo.getContentType());
        documento.setTamano(archivo.getSize());
        documento.setDescripcion(descripcion);

        Documento documentoGuardado = documentoRepository.save(documento);
        return convertirADTO(documentoGuardado);
    }

    @Override
    public Resource descargarDocumento(Long id) {
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado con ID: " + id));

        return fileStorageService.loadAsResource(documento.getRuta());
    }

    @Override
    @Transactional
    public void eliminarDocumento(Long id) {
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado con ID: " + id));

        try {
            // Eliminar el archivo físico
            Path archivo = fileStorageService.load(documento.getRuta());
            Files.deleteIfExists(archivo);

            // Eliminar el registro de la base de datos
            documentoRepository.delete(documento);
        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar el documento", e);
        }
    }

    private DocumentoDTO convertirADTO(Documento documento) {
        DocumentoDTO dto = new DocumentoDTO();
        dto.setId(documento.getId());
        dto.setReservaId(documento.getReserva().getId());
        dto.setNombre(documento.getNombre());
        dto.setRuta(documento.getRuta());
        dto.setTipo(documento.getTipo());
        dto.setTamano(documento.getTamano());
        dto.setDescripcion(documento.getDescripcion());
        dto.setCreatedAt(documento.getCreatedAt());
        dto.setUpdatedAt(documento.getUpdatedAt());
        return dto;
    }
}