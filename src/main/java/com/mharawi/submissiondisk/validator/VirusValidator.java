package com.mharawi.submissiondisk.validator;

import com.mharawi.submissiondisk.event.SubmissionEvent;
import com.mharawi.submissiondisk.service.VirusScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.capybara.clamav.commands.scan.result.ScanResult;

/**
 * Validates that a submission file is virus-free using ClamAV.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VirusValidator implements SubmissionValidator {

    private final VirusScanService virusScanService;

    @Override
    public ValidationResult validate(SubmissionEvent event) {
        log.debug("Starting virus scan for submission: {}", event.getSubmissionId());

        try {
            ScanResult scanResult = virusScanService.scanFile(event.getStoragePath());

            if (scanResult == null) {
                log.info("Virus scan skipped (ClamAV disabled) for submission {}", event.getSubmissionId());
                return ValidationResult.success(getValidatorName());
            }

            if (scanResult instanceof ScanResult.VirusFound virusFound) {
                log.error("VIRUS DETECTED in submission {}: {}", event.getSubmissionId(), virusFound);
                return ValidationResult.failure(
                        getValidatorName(),
                        "Virus detected: " + virusFound.getFoundViruses()
                );
            }

            if (!(scanResult instanceof ScanResult.OK)) {
                log.warn("Unexpected scan result for submission {}: {}", event.getSubmissionId(), scanResult);
                return ValidationResult.failure(
                        getValidatorName(),
                        "Unexpected virus scan result: " + scanResult.getClass().getSimpleName()
                );
            }

            log.info("Virus scan PASSED for submission {}", event.getSubmissionId());
            return ValidationResult.success(getValidatorName());

        } catch (Exception e) {
            log.error("Error during virus scan for submission {}", event.getSubmissionId(), e);
            return ValidationResult.failure(
                    getValidatorName(),
                    "Virus scan failed: " + e.getMessage()
            );
        }
    }

    @Override
    public String getValidatorName() {
        return "VirusValidator";
    }

    @Override
    public double getOrder() {
        return 20;
    }
}

