package com.mharawi.submissiondisk.validator;

import com.mharawi.submissiondisk.event.SubmissionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Validates submission filename for security and format requirements.
 * Checks for:
 * - Invalid characters
 * - Path traversal attempts
 * - Suspicious patterns
 * - Required extensions
 */
@Component
@Slf4j
public class FilenameValidator implements SubmissionValidator {

    // Pattern to detect path traversal attempts
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(".*[/\\\\]\\.\\.[\\\\/].*");

    // Pattern for invalid characters in filenames
    private static final Pattern INVALID_CHARS_PATTERN = Pattern.compile(".*[<>:\"|?*\\x00-\\x1F].*");

    // Allowed file extensions
    private static final Pattern ALLOWED_EXTENSIONS = Pattern.compile(".*\\.(zip|ZIP)$");

    // Maximum filename length
    private static final int MAX_FILENAME_LENGTH = 255;

    @Override
    public ValidationResult validate(SubmissionEvent event) {
        String originalFilename = event.getOriginalFileName();

        log.debug("Validating filename: {}", originalFilename);

        // Check for null or empty filename
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return ValidationResult.failure(getValidatorName(), "Filename cannot be empty");
        }

        // Check filename length
        if (originalFilename.length() > MAX_FILENAME_LENGTH) {
            return ValidationResult.failure(
                    getValidatorName(),
                    "Filename too long (max " + MAX_FILENAME_LENGTH + " characters)"
            );
        }

        // Check for path traversal attempts
        if (PATH_TRAVERSAL_PATTERN.matcher(originalFilename).matches()) {
            log.warn("Path traversal attempt detected in filename: {}", originalFilename);
            return ValidationResult.failure(
                    getValidatorName(),
                    "Invalid filename: path traversal attempt detected"
            );
        }

        // Check for invalid characters
        if (INVALID_CHARS_PATTERN.matcher(originalFilename).matches()) {
            log.warn("Invalid characters detected in filename: {}", originalFilename);
            return ValidationResult.failure(
                    getValidatorName(),
                    "Invalid characters in filename"
            );
        }

        // Check file extension
        if (!ALLOWED_EXTENSIONS.matcher(originalFilename).matches()) {
            return ValidationResult.failure(
                    getValidatorName(),
                    "Invalid file extension. Only .zip files are allowed"
            );
        }

        // Check for suspicious patterns (null bytes, control characters)
        if (originalFilename.contains("\0")) {
            log.warn("Null byte detected in filename: {}", originalFilename);
            return ValidationResult.failure(
                    getValidatorName(),
                    "Invalid filename: contains null bytes"
            );
        }

        log.debug("Filename validation passed for: {}", originalFilename);
        return ValidationResult.success(getValidatorName());
    }

    @Override
    public String getValidatorName() {
        return "FilenameValidator";
    }

    @Override
    public double getOrder() {
        return 5;
    }
}


