package com.logistica.agendamiento.service.impl;

import com.logistica.agendamiento.dto.AreaDTO;
import com.logistica.agendamiento.entity.Area;
import com.logistica.agendamiento.exception.ResourceAlreadyExistsException;
import com.logistica.agendamiento.exception.ResourceNotFoundException;
import com.logistica.agendamiento.repository.AreaRepository;
import com.logistica.agendamiento.service.AreaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AreaServiceImpl implements AreaService {

    private final AreaRepository areaRepository;

    @Override
    public List<AreaDTO> obtenerTodasLasAreas() {
        return areaRepository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    public AreaDTO obtenerAreaPorId(Long id) {
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Área no encontrada con ID: " + id));
        return convertirADTO(area);
    }

    @Override
    public AreaDTO crearArea(AreaDTO areaDTO) {
        // Verificar que no exista un área con el mismo nombre
        if (areaRepository.existsByNombre(areaDTO.getNombre())) {
            throw new ResourceAlreadyExistsException("Ya existe un área con el nombre: " + areaDTO.getNombre());
        }

        Area area = new Area();
        area.setNombre(areaDTO.getNombre());
        area.setDescripcion(areaDTO.getDescripcion());

        Area areaSaved = areaRepository.save(area);
        return convertirADTO(areaSaved);
    }

    @Override
    public AreaDTO actualizarArea(Long id, AreaDTO areaDTO) {
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Área no encontrada con ID: " + id));

        // Verificar que no exista otra área con el mismo nombre
        if (!area.getNombre().equals(areaDTO.getNombre()) &&
                areaRepository.existsByNombre(areaDTO.getNombre())) {
            throw new ResourceAlreadyExistsException("Ya existe un área con el nombre: " + areaDTO.getNombre());
        }

        area.setNombre(areaDTO.getNombre());
        area.setDescripcion(areaDTO.getDescripcion());

        Area areaActualizada = areaRepository.save(area);
        return convertirADTO(areaActualizada);
    }

    @Override
    public void eliminarArea(Long id) {
        if (!areaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Área no encontrada con ID: " + id);
        }
        areaRepository.deleteById(id);
    }

    private AreaDTO convertirADTO(Area area) {
        AreaDTO dto = new AreaDTO();
        dto.setId(area.getId());
        dto.setNombre(area.getNombre());
        dto.setDescripcion(area.getDescripcion());
        return dto;
    }
}