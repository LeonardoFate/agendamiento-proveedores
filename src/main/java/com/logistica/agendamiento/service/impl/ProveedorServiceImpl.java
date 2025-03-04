package com.logistica.agendamiento.service.impl;

import com.logistica.agendamiento.dto.ProveedorDTO;
import com.logistica.agendamiento.entity.Proveedor;
import com.logistica.agendamiento.exception.ResourceAlreadyExistsException;
import com.logistica.agendamiento.exception.ResourceNotFoundException;
import com.logistica.agendamiento.repository.ProveedorRepository;
import com.logistica.agendamiento.service.ProveedorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;

    @Override
    public List<ProveedorDTO> obtenerTodosLosProveedores() {
        return proveedorRepository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    public ProveedorDTO obtenerProveedorPorId(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + id));
        return convertirADTO(proveedor);
    }

    @Override
    public ProveedorDTO obtenerProveedorPorRuc(String ruc) {
        Proveedor proveedor = proveedorRepository.findByRuc(ruc)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con RUC: " + ruc));
        return convertirADTO(proveedor);
    }

    @Override
    public ProveedorDTO actualizarProveedor(Long id, ProveedorDTO proveedorDTO) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + id));

        // Verificar que el RUC no esté en uso por otro proveedor
        if (!proveedor.getRuc().equals(proveedorDTO.getRuc()) &&
                proveedorRepository.existsByRuc(proveedorDTO.getRuc())) {
            throw new ResourceAlreadyExistsException("El RUC ya está registrado: " + proveedorDTO.getRuc());
        }

        // Verificar que el email no esté en uso por otro proveedor
        if (!proveedor.getEmail().equals(proveedorDTO.getEmail()) &&
                proveedorRepository.existsByEmail(proveedorDTO.getEmail())) {
            throw new ResourceAlreadyExistsException("El email ya está registrado: " + proveedorDTO.getEmail());
        }

        proveedor.setNombre(proveedorDTO.getNombre());
        proveedor.setRuc(proveedorDTO.getRuc());
        proveedor.setDireccion(proveedorDTO.getDireccion());
        proveedor.setTelefono(proveedorDTO.getTelefono());
        proveedor.setEmail(proveedorDTO.getEmail());

        Proveedor proveedorActualizado = proveedorRepository.save(proveedor);
        return convertirADTO(proveedorActualizado);
    }

    @Override
    public ProveedorDTO cambiarEstadoProveedor(Long id, Boolean estado) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + id));

        proveedor.setEstado(estado);

        // También actualizar el estado del usuario asociado
        if (proveedor.getUsuario() != null) {
            proveedor.getUsuario().setEstado(estado);
        }

        Proveedor proveedorActualizado = proveedorRepository.save(proveedor);
        return convertirADTO(proveedorActualizado);
    }

    private ProveedorDTO convertirADTO(Proveedor proveedor) {
        ProveedorDTO dto = new ProveedorDTO();
        dto.setId(proveedor.getId());
        dto.setNombre(proveedor.getNombre());
        dto.setRuc(proveedor.getRuc());
        dto.setDireccion(proveedor.getDireccion());
        dto.setTelefono(proveedor.getTelefono());
        dto.setEmail(proveedor.getEmail());
        dto.setEstado(proveedor.getEstado());

        if (proveedor.getUsuario() != null) {
            dto.setUsuarioId(proveedor.getUsuario().getId());
        }

        return dto;
    }
}