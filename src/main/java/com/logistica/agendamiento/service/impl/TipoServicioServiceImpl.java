package com.logistica.agendamiento.service.impl;

import com.logistica.agendamiento.dto.TipoServicioDTO;
import com.logistica.agendamiento.entity.TipoServicio;
import com.logistica.agendamiento.exception.ResourceAlreadyExistsException;
import com.logistica.agendamiento.exception.ResourceNotFoundException;
import com.logistica.agendamiento.repository.TipoServicioRepository;
import com.logistica.agendamiento.service.TipoServicioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TipoServicioServiceImpl implements TipoServicioService {

    private final TipoServicioRepository tipoServicioRepository;

    @Override
    public List<TipoServicioDTO> obtenerTodosTiposServicio() {
        return tipoServicioRepository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    public TipoServicioDTO obtenerTipoServicioPorId(Long id) {
        TipoServicio tipoServicio = tipoServicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de servicio no encontrado con ID: " + id));
        return convertirADTO(tipoServicio);
    }

    @Override
    public TipoServicioDTO crearTipoServicio(TipoServicioDTO tipoServicioDTO) {
        // Verificar que no exista un tipo de servicio con el mismo nombre
        if (tipoServicioRepository.existsByNombre(tipoServicioDTO.getNombre())) {
            throw new ResourceAlreadyExistsException("Ya existe un tipo de servicio con el nombre: " + tipoServicioDTO.getNombre());
        }

        TipoServicio tipoServicio = new TipoServicio();
        tipoServicio.setNombre(tipoServicioDTO.getNombre());
        tipoServicio.setDescripcion(tipoServicioDTO.getDescripcion());

        TipoServicio tipoServicioSaved = tipoServicioRepository.save(tipoServicio);
        return convertirADTO(tipoServicioSaved);
    }

    @Override
    public TipoServicioDTO actualizarTipoServicio(Long id, TipoServicioDTO tipoServicioDTO) {
        TipoServicio tipoServicio = tipoServicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de servicio no encontrado con ID: " + id));

        // Verificar que no exista otro tipo de servicio con el mismo nombre
        if (!tipoServicio.getNombre().equals(tipoServicioDTO.getNombre()) &&
                tipoServicioRepository.existsByNombre(tipoServicioDTO.getNombre())) {
            throw new ResourceAlreadyExistsException("Ya existe un tipo de servicio con el nombre: " + tipoServicioDTO.getNombre());
        }

        tipoServicio.setNombre(tipoServicioDTO.getNombre());
        tipoServicio.setDescripcion(tipoServicioDTO.getDescripcion());

        TipoServicio tipoServicioActualizado = tipoServicioRepository.save(tipoServicio);
        return convertirADTO(tipoServicioActualizado);
    }

    @Override
    public void eliminarTipoServicio(Long id) {
        if (!tipoServicioRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tipo de servicio no encontrado con ID: " + id);
        }
        tipoServicioRepository.deleteById(id);
    }

    private TipoServicioDTO convertirADTO(TipoServicio tipoServicio) {
        TipoServicioDTO dto = new TipoServicioDTO();
        dto.setId(tipoServicio.getId());
        dto.setNombre(tipoServicio.getNombre());
        dto.setDescripcion(tipoServicio.getDescripcion());
        return dto;
    }
}