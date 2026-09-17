package com.kbase.backend.document;

import com.kbase.backend.exception.BadRequestException;
import com.kbase.backend.exception.FileTooLargeException;
import com.kbase.backend.exception.UnsupportedMediaTypeException;
import com.kbase.backend.storage.StorageProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

@Component
public class DocumentFileValidator {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "text/plain",
            "text/markdown",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "image/png",
            "image/jpeg"
    );

    private final StorageProperties properties;

    public DocumentFileValidator(StorageProperties properties) {
        this.properties = properties;
    }

    public ValidatedFile validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File must not be empty");
        }
        if (file.getSize() > properties.maxFileSize()) {
            throw new FileTooLargeException("File exceeds the configured maximum size");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BadRequestException("Original file name is required");
        }
        originalName = originalName.trim();
        if (originalName.length() > 500 || originalName.contains("/")
                || originalName.contains("\\") || originalName.indexOf('\0') >= 0
                || originalName.equals(".") || originalName.equals("..")) {
            throw new BadRequestException("Invalid file name");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(
                contentType.toLowerCase(Locale.ROOT))) {
            throw new UnsupportedMediaTypeException("Unsupported file content type");
        }

        String safeName = originalName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (safeName.isBlank()) {
            throw new BadRequestException("Invalid file name");
        }
        return new ValidatedFile(originalName, safeName, contentType.toLowerCase(Locale.ROOT),
                file.getSize());
    }

    public record ValidatedFile(
            String originalFileName,
            String safeFileName,
            String contentType,
            long size
    ) {
    }
}
