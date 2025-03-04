package com.logistica.agendamiento.service.impl;

import com.logistica.agendamiento.config.FileStorageProperties;
import com.logistica.agendamiento.exception.FileStorageException;
import com.logistica.agendamiento.exception.ResourceNotFoundException;
import com.logistica.agendamiento.service.FileStorageService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path rootLocation;

    public FileStorageServiceImpl(FileStorageProperties properties) {
        this.rootLocation = Paths.get(properties.getUploadDir());
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new FileStorageException("No se pudo inicializar el directorio de almacenamiento", e);
        }
    }

    @Override
    public String store(MultipartFile file, Long reservaId) {
        try {
            if (file.isEmpty()) {
                throw new FileStorageException("No se puede almacenar un archivo vacío");
            }

            // Crear directorio para la reserva si no existe
            Path reservaDir = rootLocation.resolve(reservaId.toString());
            if (!Files.exists(reservaDir)) {
                Files.createDirectories(reservaDir);
            }

            // Generar nombre único para el archivo
            String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String extension = FilenameUtils.getExtension(originalFileName);
            String newFileName = UUID.randomUUID().toString() + "." + extension;

            Path destinationFile = reservaDir.resolve(Paths.get(newFileName))
                    .normalize().toAbsolutePath();

            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return reservaId + "/" + newFileName;
        } catch (IOException e) {
            throw new FileStorageException("Error al almacenar el archivo", e);
        }
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.rootLocation, 1)
                    .filter(path -> !path.equals(this.rootLocation))
                    .map(this.rootLocation::relativize);
        } catch (IOException e) {
            throw new FileStorageException("Error al leer los archivos almacenados", e);
        }
    }

    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("No se pudo leer el archivo: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("No se pudo leer el archivo: " + filename, e);
        }
    }

    @Override
    public void deleteAll() {
        try {
            Files.walk(rootLocation)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new FileStorageException("Error al eliminar archivos", e);
                        }
                    });
        } catch (IOException e) {
            throw new FileStorageException("Error al eliminar archivos", e);
        }
    }
}