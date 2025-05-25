package com.logistica.agendamiento.controller;

import com.logistica.agendamiento.dto.PlantillaHorarioDTO;
import com.logistica.agendamiento.entity.PlantillaHorario;
import com.logistica.agendamiento.entity.enums.DiaSemana;
import com.logistica.agendamiento.service.PlantillaHorarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/plantillas")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class PlantillaHorarioController {

    private final PlantillaHorarioService plantillaHorarioService;

    // Subir Excel con plantilla
    @PostMapping("/upload-excel")
    public ResponseEntity<?> subirPlantillaExcel(@RequestParam("archivo") MultipartFile archivo) {
        try {
            List<PlantillaHorario> plantillas = plantillaHorarioService.cargarDesdeExcel(archivo);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Plantilla cargada exitosamente",
                    "plantillasCargadas", plantillas.size(),
                    "plantillas", plantillaHorarioService.convertirADTOList(plantillas)
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Error al procesar el archivo",
                    "detalle", e.getMessage()
            ));
        }
    }

    // Ver plantilla actual completa
    @GetMapping("/semana")
    public ResponseEntity<List<PlantillaHorarioDTO>> obtenerPlantillaSemana() {
        List<PlantillaHorario> plantillas = plantillaHorarioService.obtenerTodasLasPlantillas();
        return ResponseEntity.ok(plantillaHorarioService.convertirADTOList(plantillas));
    }

    // Ver plantilla por día específico
    @GetMapping("/dia/{dia}")
    public ResponseEntity<List<PlantillaHorarioDTO>> obtenerPlantillaPorDia(@PathVariable DiaSemana dia) {
        List<PlantillaHorario> plantillas = plantillaHorarioService.obtenerPlantillaPorDia(dia);
        return ResponseEntity.ok(plantillaHorarioService.convertirADTOList(plantillas));
    }

    // CRUD individual
    @PostMapping
    public ResponseEntity<PlantillaHorarioDTO> crearHorario(@Valid @RequestBody PlantillaHorarioDTO dto) {
        PlantillaHorario plantilla = plantillaHorarioService.crearHorario(dto);
        return new ResponseEntity<>(plantillaHorarioService.convertirADTO(plantilla), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlantillaHorarioDTO> actualizarHorario(
            @PathVariable Long id,
            @Valid @RequestBody PlantillaHorarioDTO dto) {
        PlantillaHorario plantilla = plantillaHorarioService.actualizarHorario(id, dto);
        return ResponseEntity.ok(plantillaHorarioService.convertirADTO(plantilla));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarHorario(@PathVariable Long id) {
        plantillaHorarioService.eliminarHorario(id);
        return ResponseEntity.noContent().build();
    }

    // Obtener estadísticas de la plantilla
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        List<PlantillaHorario> todasPlantillas = plantillaHorarioService.obtenerTodasLasPlantillas();

        long totalProveedores = todasPlantillas.stream()
                .map(p -> p.getProveedor().getId())
                .distinct()
                .count();

        Map<DiaSemana, Long> porDia = todasPlantillas.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        PlantillaHorario::getDia,
                        java.util.stream.Collectors.counting()
                ));

        return ResponseEntity.ok(Map.of(
                "totalPlantillas", todasPlantillas.size(),
                "totalProveedores", totalProveedores,
                "distribucionPorDia", porDia
        ));
    }

    @DeleteMapping("/bulk-delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> eliminarPlantillasMultiple(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "La lista de IDs está vacía o no fue enviada"
            ));
        }

        try {
            plantillaHorarioService.eliminarHorariosMultiple(ids);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Plantillas eliminadas exitosamente",
                    "eliminadas", ids.size()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Uno o más IDs no existen",
                    "detalle", e.getMessage()
            ));
        } catch (Exception e) {
            e.printStackTrace(); // Para ver el error completo en consola
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Error inesperado al eliminar plantillas",
                    "detalle", e.getMessage()
            ));
        }
    }


}