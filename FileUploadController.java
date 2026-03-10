package com.fileupload.controller;

import com.fileupload.exception.FileUploadException;
import com.fileupload.exception.InvalidFileException;
import com.fileupload.model.FileMetadata;
import com.fileupload.model.UploadResponse;
import com.fileupload.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/")
public class FileUploadController {

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

    private final FileStorageService fileStorageService;

    public FileUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    // ─── View: Main Upload Page ────────────────────────────────────────────────

    @GetMapping
    public String index(Model model) {
        model.addAttribute("files", fileStorageService.listAll());
        return "index";
    }

    // ─── REST: Single File Upload ─────────────────────────────────────────────

    @PostMapping(value = "/api/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<UploadResponse> uploadSingle(
            @RequestParam("file") MultipartFile file) {

        log.debug("Single upload request: {}", file.getOriginalFilename());
        try {
            FileMetadata metadata = fileStorageService.store(file);
            return ResponseEntity.ok(UploadResponse.builder()
                .success(true)
                .message("File uploaded successfully")
                .files(List.of(metadata))
                .totalFiles(1)
                .totalSize(metadata.getSize())
                .build());
        } catch (InvalidFileException e) {
            log.warn("Invalid file upload: {}", e.getMessage());
            return ResponseEntity.badRequest().body(UploadResponse.builder()
                .success(false)
                .message(e.getMessage())
                .errors(List.of(e.getMessage()))
                .build());
        } catch (Exception e) {
            log.error("Upload error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(UploadResponse.builder()
                    .success(false)
                    .message("Upload failed: " + e.getMessage())
                    .errors(List.of(e.getMessage()))
                    .build());
        }
    }

    // ─── REST: Multiple Files Upload ──────────────────────────────────────────

    @PostMapping(value = "/api/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<UploadResponse> uploadMultiple(
            @RequestParam("files") List<MultipartFile> files) {

        log.debug("Multiple upload request: {} files", files.size());

        List<FileMetadata> uploaded = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        long totalSize = 0;

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            try {
                FileMetadata metadata = fileStorageService.store(file);
                uploaded.add(metadata);
                totalSize += metadata.getSize();
            } catch (InvalidFileException | FileUploadException e) {
                log.warn("File rejected: {} - {}", file.getOriginalFilename(), e.getMessage());
                errors.add(file.getOriginalFilename() + ": " + e.getMessage());
            }
        }

        boolean hasSuccesses = !uploaded.isEmpty();
        return ResponseEntity.status(hasSuccesses ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
            .body(UploadResponse.builder()
                .success(hasSuccesses)
                .message(hasSuccesses
                    ? uploaded.size() + " file(s) uploaded successfully"
                    : "All uploads failed")
                .files(uploaded)
                .totalFiles(uploaded.size())
                .totalSize(totalSize)
                .errors(errors)
                .build());
    }

    // ─── REST: Download File ──────────────────────────────────────────────────

    @GetMapping("/api/files/{fileId}/download")
    public ResponseEntity<Resource> download(@PathVariable String fileId) {
        try {
            Resource resource = fileStorageService.loadAsResource(fileId);
            FileMetadata metadata = fileStorageService.findById(fileId)
                .orElseThrow(() -> new FileUploadException("File not found"));

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                    metadata.getContentType() != null ? metadata.getContentType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + metadata.getOriginalName() + "\"")
                .body(resource);
        } catch (FileUploadException e) {
            log.warn("Download failed for {}: {}", fileId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // ─── REST: Delete File ────────────────────────────────────────────────────

    @DeleteMapping("/api/files/{fileId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String fileId) {
        boolean deleted = fileStorageService.delete(fileId);
        if (deleted) {
            return ResponseEntity.ok(Map.of("success", true, "message", "File deleted"));
        }
        return ResponseEntity.notFound().build();
    }

    // ─── REST: List All Files ─────────────────────────────────────────────────

    @GetMapping("/api/files")
    @ResponseBody
    public ResponseEntity<List<FileMetadata>> listFiles() {
        return ResponseEntity.ok(fileStorageService.listAll());
    }

    // ─── Exception Handlers ───────────────────────────────────────────────────

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    @ResponseBody
    public ResponseEntity<UploadResponse> handleMaxSizeExceeded(Exception e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(UploadResponse.builder()
                .success(false)
                .message("File size exceeds the maximum allowed limit (50MB)")
                .errors(List.of("File too large"))
                .build());
    }
}
