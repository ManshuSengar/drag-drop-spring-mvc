package com.fileupload.service;

import com.fileupload.exception.InvalidFileException;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Service
public class FileValidationService {

    private static final Logger log = LoggerFactory.getLogger(FileValidationService.class);

    @Value("${app.upload.max-file-size:52428800}")
    private long maxFileSize;

    @Value("${app.upload.allowed-extensions:jpg,jpeg,png,gif,pdf,doc,docx,txt,csv,zip}")
    private String allowedExtensions;

    private static final List<String> DANGEROUS_EXTENSIONS = Arrays.asList(
        "exe", "bat", "cmd", "sh", "php", "asp", "aspx", "js", "vbs", "ps1"
    );

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is empty or null");
        }
        validateSize(file);
        validateExtension(file);
        validateContentType(file);
        validateMagicBytes(file);
    }

    private void validateSize(MultipartFile file) {
        if (file.getSize() > maxFileSize) {
            throw new InvalidFileException(
                String.format("File '%s' exceeds maximum allowed size of %s",
                    file.getOriginalFilename(), formatSize(maxFileSize))
            );
        }
    }

    private void validateExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new InvalidFileException("Filename is invalid");
        }

        String extension = FilenameUtils.getExtension(filename).toLowerCase();

        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException("File type '" + extension + "' is not allowed for security reasons");
        }

        List<String> allowed = Arrays.asList(allowedExtensions.split(","));
        if (!allowed.contains(extension)) {
            throw new InvalidFileException(
                String.format("File extension '.%s' is not allowed. Allowed: %s", extension, allowedExtensions)
            );
        }
    }

    private void validateContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new InvalidFileException("Cannot determine file content type");
        }
        if (contentType.contains("application/x-msdownload") ||
            contentType.contains("application/x-executable")) {
            throw new InvalidFileException("Executable files are not allowed");
        }
    }

    private void validateMagicBytes(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int bytesRead = is.read(header);
            if (bytesRead < 4) return;

            // Check for EXE magic bytes: MZ header
            if (header[0] == 0x4D && header[1] == 0x5A) {
                throw new InvalidFileException("Executable files are disguised and not allowed");
            }

            // Check for PHP tags in file
            String headerStr = new String(header, 0, bytesRead);
            if (headerStr.contains("<?php") || headerStr.contains("<%")) {
                throw new InvalidFileException("Script files are not allowed");
            }
        } catch (InvalidFileException e) {
            throw e;
        } catch (IOException e) {
            log.warn("Could not read file header for validation: {}", e.getMessage());
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }
}
