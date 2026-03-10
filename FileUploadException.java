package com.fileupload.exception;

public class FileUploadException extends RuntimeException {
    private final String errorCode;

    public FileUploadException(String message) {
        super(message);
        this.errorCode = "FILE_UPLOAD_ERROR";
    }

    public FileUploadException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "FILE_UPLOAD_ERROR";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
