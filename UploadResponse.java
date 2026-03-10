package com.fileupload.model;

import java.util.List;

public class UploadResponse {

    private boolean success;
    private String message;
    private List<FileMetadata> files;
    private int totalFiles;
    private long totalSize;
    private List<String> errors;

    public UploadResponse() {
    }

    public UploadResponse(boolean success,
                          String message,
                          List<FileMetadata> files,
                          int totalFiles,
                          long totalSize,
                          List<String> errors) {
        this.success = success;
        this.message = message;
        this.files = files;
        this.totalFiles = totalFiles;
        this.totalSize = totalSize;
        this.errors = errors;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<FileMetadata> getFiles() {
        return files;
    }

    public void setFiles(List<FileMetadata> files) {
        this.files = files;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public static class Builder {
        private final UploadResponse instance = new UploadResponse();

        public Builder success(boolean success) {
            instance.setSuccess(success);
            return this;
        }

        public Builder message(String message) {
            instance.setMessage(message);
            return this;
        }

        public Builder files(List<FileMetadata> files) {
            instance.setFiles(files);
            return this;
        }

        public Builder totalFiles(int totalFiles) {
            instance.setTotalFiles(totalFiles);
            return this;
        }

        public Builder totalSize(long totalSize) {
            instance.setTotalSize(totalSize);
            return this;
        }

        public Builder errors(List<String> errors) {
            instance.setErrors(errors);
            return this;
        }

        public UploadResponse build() {
            return instance;
        }
    }
}
