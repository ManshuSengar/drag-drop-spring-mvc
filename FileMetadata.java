package com.fileupload.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileMetadata {
    private String id;
    private String originalName;
    private String storedName;
    private String contentType;
    private long size;
    private String uploadPath;
    private LocalDateTime uploadedAt;
    private String fileExtension;
    private String fileCategory;

    public FileMetadata() {
    }

    public FileMetadata(String id,
                        String originalName,
                        String storedName,
                        String contentType,
                        long size,
                        String uploadPath,
                        LocalDateTime uploadedAt,
                        String fileExtension,
                        String fileCategory) {
        this.id = id;
        this.originalName = originalName;
        this.storedName = storedName;
        this.contentType = contentType;
        this.size = size;
        this.uploadPath = uploadPath;
        this.uploadedAt = uploadedAt;
        this.fileExtension = fileExtension;
        this.fileCategory = fileCategory;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getStoredName() {
        return storedName;
    }

    public void setStoredName(String storedName) {
        this.storedName = storedName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getUploadPath() {
        return uploadPath;
    }

    public void setUploadPath(String uploadPath) {
        this.uploadPath = uploadPath;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getFileCategory() {
        return fileCategory;
    }

    public void setFileCategory(String fileCategory) {
        this.fileCategory = fileCategory;
    }

    public String getFormattedSize() {
        if (size < 1024) return size + " B";
        else if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        else if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        else return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    public String getFormattedUploadDate() {
        if (uploadedAt == null) return "";
        return uploadedAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
    }

    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    public static class Builder {
        private final FileMetadata instance = new FileMetadata();

        public Builder id(String id) {
            instance.setId(id);
            return this;
        }

        public Builder originalName(String originalName) {
            instance.setOriginalName(originalName);
            return this;
        }

        public Builder storedName(String storedName) {
            instance.setStoredName(storedName);
            return this;
        }

        public Builder contentType(String contentType) {
            instance.setContentType(contentType);
            return this;
        }

        public Builder size(long size) {
            instance.setSize(size);
            return this;
        }

        public Builder uploadPath(String uploadPath) {
            instance.setUploadPath(uploadPath);
            return this;
        }

        public Builder uploadedAt(LocalDateTime uploadedAt) {
            instance.setUploadedAt(uploadedAt);
            return this;
        }

        public Builder fileExtension(String fileExtension) {
            instance.setFileExtension(fileExtension);
            return this;
        }

        public Builder fileCategory(String fileCategory) {
            instance.setFileCategory(fileCategory);
            return this;
        }

        public FileMetadata build() {
            return instance;
        }
    }
}
