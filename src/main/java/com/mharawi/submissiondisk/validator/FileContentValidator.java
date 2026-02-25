package com.mharawi.submissiondisk.validator;

import com.mharawi.submissiondisk.event.SubmissionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

/**
 * Validates ZIP file structure and content.
 * Checks for:
 * - Valid ZIP format
 * - Zip bombs (excessive compression ratio)
 * - Path traversal in entries
 * - Maximum entry count
 */
@Component
@Slf4j
public class FileContentValidator implements SubmissionValidator {

    @Value("${submission.max-zip-entries:10000}")
    private int maxZipEntries;

    @Value("${submission.max-compression-ratio:100}")
    private int maxCompressionRatio;

    @Override
    public ValidationResult validate(SubmissionEvent event) {
        log.debug("Validating file content for submission: {}", event.getSubmissionId());

        Path filePath = Paths.get(event.getStoragePath());

        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(filePath))) {

            int entryCount = 0;
            long totalUncompressedSize = 0;
            long totalCompressedSize = event.getFileSize();

            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                entryCount++;

                // Check maximum entry count (prevent zip bombs)
                if (entryCount > maxZipEntries) {
                    log.warn("Too many ZIP entries in submission {}: {}", event.getSubmissionId(), entryCount);
                    return ValidationResult.failure(
                            getValidatorName(),
                            "ZIP file contains too many entries (maximum: " + maxZipEntries + ")"
                    );
                }

                // Check for path traversal in entry names
                String entryName = entry.getName();
                if (entryName.contains("..") || entryName.startsWith("/") || entryName.contains("\\")) {
                    log.warn("Suspicious entry name detected in submission {}: {}", event.getSubmissionId(), entryName);
                    return ValidationResult.failure(
                            getValidatorName(),
                            "ZIP contains invalid entry path: " + entryName
                    );
                }

                // Track uncompressed size for compression ratio check
                if (!entry.isDirectory()) {
                    totalUncompressedSize += entry.getSize();
                }

                zipInputStream.closeEntry();
            }

            // Check if ZIP is empty
            if (entryCount == 0) {
                return ValidationResult.failure(
                        getValidatorName(),
                        "ZIP file is empty"
                );
            }

            // Check compression ratio (detect zip bombs)
            if (totalCompressedSize > 0 && totalUncompressedSize > 0) {
                long compressionRatio = totalUncompressedSize / totalCompressedSize;
                if (compressionRatio > maxCompressionRatio) {
                    log.warn("Suspicious compression ratio detected in submission {}: {}",
                            event.getSubmissionId(), compressionRatio);
                    return ValidationResult.failure(
                            getValidatorName(),
                            "Suspicious compression ratio detected (possible zip bomb)"
                    );
                }
            }

            log.debug("File content validation passed: {} entries", entryCount);
            return ValidationResult.success(getValidatorName());

        } catch (ZipException e) {
            log.error("Invalid ZIP file for submission {}", event.getSubmissionId(), e);
            return ValidationResult.failure(
                    getValidatorName(),
                    "Invalid ZIP file format: " + e.getMessage()
            );
        } catch (IOException e) {
            log.error("Error reading file for submission {}", event.getSubmissionId(), e);
            return ValidationResult.failure(
                    getValidatorName(),
                    "Error reading file: " + e.getMessage()
            );
        }
    }

    @Override
    public String getValidatorName() {
        return "FileContentValidator";
    }

    @Override
    public double getOrder() {
        return 15;
    }
}

