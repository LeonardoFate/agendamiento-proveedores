package com.logistica.agendamiento.service.impl;

import com.logistica.agendamiento.dto.AndenDTO;
import com.logistica.agendamiento.entity.Anden;
import com.logistica.agendamiento.entity.Area;
import com.logistica.agendamiento.entity.enums.EstadoAnden;
import com.logistica.agendamiento.exception.ResourceAlreadyExistsException;
import com.logistica.agendamiento.exception.ResourceNotFoundException;
import com.logistica.agendamiento.repository.AndenRepository;
import com.logistica.agendamiento.repository.AreaRepository;
import com.logistica.agendamiento.service.AndenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AndenServiceImpl implements AndenService {

    private final AndenRepository andenRepository;
    private final AreaRepository areaRepository;

    @Override
    public List<AndenDTO> obtenerTodosLosAndenes() {
        return andenRepository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AndenDTO> obtenerAndenesPorArea(Long areaId) {
        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("Área no encontrada con ID: " + areaId));

        return andenRepository.findByArea(area).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AndenDTO> obtenerAndenesPorAreaYEstado(Long areaId, EstadoAnden estado) {
        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("Área no encontrada con ID: " + areaId));

        return andenRepository.findByAreaAndEstado(area, estado).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }
    @Override
    public AndenDTO obtenerAndenPorId(Long id) {
        Anden anden = andenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Andén no encontrado con ID: " + id));
        return convertirADTO(anden);
    }

    @Override
    public AndenDTO crearAnden(AndenDTO andenDTO) {
        Area area = areaRepository.findById(andenDTO.getAreaId())
                .orElseThrow(() -> new ResourceNotFoundException("Área no encontrada con ID: " + andenDTO.getAreaId()));

        // Verificar que no exista un andén con el mismo número en la misma área
        if (andenRepository.existsByAreaAndNumero(area, andenDTO.getNumero())) {
            throw new ResourceAlreadyExistsException("Ya existe un andén con el número " +
                    andenDTO.getNumero() + " en el área " + area.getNombre());
        }

        Anden anden = new Anden();
        anden.setArea(area);
        anden.setNumero(andenDTO.getNumero());
        anden.setEstado(andenDTO.getEstado());
        anden.setCapacidad(andenDTO.getCapacidad());
        anden.setExclusivoContenedor(andenDTO.getExclusivoContenedor());

        Anden andenSaved = andenRepository.save(anden);
        return convertirADTO(andenSaved);
    }

    @Override
    public AndenDTO actualizarAnden(Long id, AndenDTO andenDTO) {
        Anden anden = andenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Andén no encontrado con ID: " + id));

        Area area = areaRepository.findById(andenDTO.getAreaId())
                .orElseThrow(() -> new ResourceNotFoundException("Área no encontrada con ID: " + andenDTO.getAreaId()));

        // Verificar que no exista otro andén con el mismo número en la misma área
        if ((anden.getArea().getId().equals(area.getId()) && !anden.getNumero().equals(andenDTO.getNumero())) ||
                !anden.getArea().getId().equals(area.getId())) {
            if (andenRepository.existsByAreaAndNumero(area, andenDTO.getNumero())) {
                throw new ResourceAlreadyExistsException("Ya existe un andén con el número " +
                        andenDTO.getNumero() + " en el área " + area.getNombre());
            }
        }

        anden.setArea(area);
        anden.setNumero(andenDTO.getNumero());
        anden.setEstado(andenDTO.getEstado());
        anden.setCapacidad(andenDTO.getCapacidad());
        anden.setExclusivoContenedor(andenDTO.getExclusivoContenedor());

        Anden andenActualizado = andenRepository.save(anden);
        return convertirADTO(andenActualizado);
    }

    @Override
    public AndenDTO actualizarEstadoAnden(Long id, EstadoAnden estado) {
        Anden anden = andenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Andén no encontrado con ID: " + id));

        anden.setEstado(estado);
        Anden andenActualizado = andenRepository.save(anden);
        return convertirADTO(andenActualizado);
    }

    @Override
    public void eliminarAnden(Long id) {
        if (!andenRepository.existsById(id)) {
            throw new ResourceNotFoundException("Andén no encontrado con ID: " + id);
        }
        andenRepository.deleteById(id);
    }

    private AndenDTO convertirADTO(Anden anden) {
        AndenDTO dto = new AndenDTO();
        dto.setId(anden.getId());
        dto.setAreaId(anden.getArea().getId());
        dto.setAreaNombre(anden.getArea().getNombre());
        dto.setNumero(anden.getNumero());
        dto.setEstado(anden.getEstado());
        dto.setCapacidad(anden.getCapacidad());
        dto.setExclusivoContenedor(anden.getExclusivoContenedor());
        return dto;
    }
}