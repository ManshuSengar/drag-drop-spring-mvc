package com.fileupload.service;

import com.fileupload.exception.FileUploadException;
import com.fileupload.model.FileMetadata;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path uploadPath;
    private final FileValidationService validationService;

    // In-memory store — replace with JPA/DB in production
    private final Map<String, FileMetadata> fileRegistry = new ConcurrentHashMap<>();

    @Autowired
    public FileStorageService(Path uploadPath, FileValidationService validationService) {
        this.uploadPath = uploadPath;
        this.validationService = validationService;
    }

    /**
     * Store a single file after validation.
     */
    public FileMetadata store(MultipartFile file) {
        validationService.validate(file);

        String originalFilename = StringUtils.cleanPath(
            Objects.requireNonNull(file.getOriginalFilename())
        );

        // Prevent path traversal attack
        if (originalFilename.contains("..")) {
            throw new FileUploadException("Filename contains invalid path sequence: " + originalFilename);
        }

        String fileId = UUID.randomUUID().toString();
        String extension = FilenameUtils.getExtension(originalFilename).toLowerCase();
        String storedName = fileId + (extension.isEmpty() ? "" : "." + extension);

        try {
            Path targetPath = uploadPath.resolve(storedName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            FileMetadata metadata = FileMetadata.builder()
                .id(fileId)
                .originalName(originalFilename)
                .storedName(storedName)
                .contentType(file.getContentType())
                .size(file.getSize())
                .uploadPath(targetPath.toString())
                .uploadedAt(LocalDateTime.now())
                .fileExtension(extension)
                .fileCategory(resolveCategory(extension))
                .build();

            fileRegistry.put(fileId, metadata);
            log.info("File stored: {} -> {}", originalFilename, storedName);
            return metadata;

        } catch (IOException e) {
            throw new FileUploadException("Failed to store file: " + originalFilename, e);
        }
    }

    /**
     * Store multiple files.
     */
    public List<FileMetadata> storeAll(List<MultipartFile> files) {
        return files.stream()
            .filter(f -> f != null && !f.isEmpty())
            .map(this::store)
            .collect(Collectors.toList());
    }

    /**
     * Load file as a downloadable resource.
     */
    public Resource loadAsResource(String fileId) {
        FileMetadata metadata = fileRegistry.get(fileId);
        if (metadata == null) {
            throw new FileUploadException("File not found with id: " + fileId, "FILE_NOT_FOUND");
        }

        try {
            Path filePath = uploadPath.resolve(metadata.getStoredName()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new FileUploadException("File not readable: " + fileId, "FILE_NOT_READABLE");

        } catch (MalformedURLException e) {
            throw new FileUploadException("Invalid file path for id: " + fileId, e);
        }
    }

    /**
     * Delete a file by ID.
     */
    public boolean delete(String fileId) {
        FileMetadata metadata = fileRegistry.get(fileId);
        if (metadata == null) return false;

        try {
            Path filePath = uploadPath.resolve(metadata.getStoredName());
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                fileRegistry.remove(fileId);
                log.info("File deleted: {}", metadata.getOriginalName());
            }
            return deleted;
        } catch (IOException e) {
            log.error("Error deleting file {}: {}", fileId, e.getMessage());
            return false;
        }
    }

    /**
     * List all uploaded files.
     */
    public List<FileMetadata> listAll() {
        return fileRegistry.values().stream()
            .sorted(Comparator.comparing(FileMetadata::getUploadedAt).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Get file metadata by ID.
     */
    public Optional<FileMetadata> findById(String fileId) {
        return Optional.ofNullable(fileRegistry.get(fileId));
    }

    private String resolveCategory(String extension) {
        Set<String> images = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg");
        Set<String> documents = Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv");
        Set<String> archives = Set.of("zip", "rar", "7z", "tar", "gz");
        Set<String> videos = Set.of("mp4", "avi", "mkv", "mov", "wmv");
        Set<String> audio = Set.of("mp3", "wav", "flac", "aac", "ogg");

        if (images.contains(extension)) return "IMAGE";
        if (documents.contains(extension)) return "DOCUMENT";
        if (archives.contains(extension)) return "ARCHIVE";
        if (videos.contains(extension)) return "VIDEO";
        if (audio.contains(extension)) return "AUDIO";
        return "OTHER";
    }
}
