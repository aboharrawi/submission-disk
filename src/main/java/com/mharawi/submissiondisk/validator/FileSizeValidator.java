package com.mharawi.submissiondisk.validator;

import com.mharawi.submissiondisk.event.SubmissionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Validates file size limits and file existence.
 */
@Component
@Slf4j
public class FileSizeValidator implements SubmissionValidator {

    @Value("${submission.max-file-size:104857600}") // Default 100MB
    private long maxFileSize;

    @Value("${submission.min-file-size:1}") // Default 1 byte
    private long minFileSize;

    @Override
    public ValidationResult validate(SubmissionEvent event) {
        log.debug("Validating file size for submission: {}", event.getSubmissionId());

        long fileSize = event.getFileSize();

        // Check minimum size
        if (fileSize < minFileSize) {
            return ValidationResult.failure(
                    getValidatorName(),
                    "File is empty or too small (minimum: " + minFileSize + " bytes)"
            );
        }

        // Check maximum size
        if (fileSize > maxFileSize) {
            return ValidationResult.failure(
                    getValidatorName(),
                    "File too large (maximum: " + formatBytes(maxFileSize) + ", received: " + formatBytes(fileSize) + ")"
            );
        }

        // Verify file exists on disk
        try {
            Path filePath = Paths.get(event.getStoragePath());
            if (!Files.exists(filePath)) {
                log.error("File not found at storage path: {}", event.getStoragePath());
                return ValidationResult.failure(
                        getValidatorName(),
                        "File not found at storage location"
                );
            }

            // Verify actual file size matches reported size
            long actualSize = Files.size(filePath);
            if (actualSize != fileSize) {
                log.warn("File size mismatch for submission {}: reported={}, actual={}",
                        event.getSubmissionId(), fileSize, actualSize);
                return ValidationResult.failure(
                        getValidatorName(),
                        "File size mismatch detected (possible corruption)"
                );
            }

        } catch (IOException e) {
            log.error("Error checking file for submission {}", event.getSubmissionId(), e);
            return ValidationResult.failure(
                    getValidatorName(),
                    "Error verifying file: " + e.getMessage()
            );
        }

        log.debug("File size validation passed: {} bytes", fileSize);
        return ValidationResult.success(getValidatorName());
    }

    @Override
    public String getValidatorName() {
        return "FileSizeValidator";
    }

    @Override
    public double getOrder() {
        return 3;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}

