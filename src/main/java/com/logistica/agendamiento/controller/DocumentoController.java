package com.logistica.agendamiento.controller;

import com.logistica.agendamiento.dto.DocumentoDTO;
import com.logistica.agendamiento.service.DocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService documentoService;

    @GetMapping("/reserva/{reservaId}")
    public ResponseEntity<List<DocumentoDTO>> obtenerDocumentosPorReservaId(@PathVariable Long reservaId) {
        return ResponseEntity.ok(documentoService.obtenerDocumentosPorReservaId(reservaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentoDTO> obtenerDocumentoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(documentoService.obtenerDocumentoPorId(id));
    }

    @PostMapping("/subir")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVEEDOR')")
    public ResponseEntity<DocumentoDTO> subirDocumento(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("reservaId") Long reservaId,
            @RequestParam(value = "descripcion", required = false) String descripcion) {

        DocumentoDTO documentoDTO = documentoService.guardarDocumento(reservaId, archivo, descripcion);
        return new ResponseEntity<>(documentoDTO, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/descargar")
    public ResponseEntity<Resource> descargarDocumento(@PathVariable Long id) {
        DocumentoDTO documento = documentoService.obtenerDocumentoPorId(id);
        Resource resource = documentoService.descargarDocumento(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(documento.getTipo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + documento.getNombre() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVEEDOR')")
    public ResponseEntity<Void> eliminarDocumento(@PathVariable Long id) {
        documentoService.eliminarDocumento(id);
        return ResponseEntity.noContent().build();
    }


}