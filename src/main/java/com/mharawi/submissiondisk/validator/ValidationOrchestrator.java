package com.mharawi.submissiondisk.validator;

import com.mharawi.submissiondisk.event.SubmissionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Orchestrates the execution of all submission validators.
 * Runs validators in order and returns the first failure encountered.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationOrchestrator {

    private final List<SubmissionValidator> validators;

    /**
     * Execute all validators in order.
     * Stops at the first failure and returns that result.
     *
     * @param event The submission event to validate
     * @return Aggregated validation result
     */
    public ValidationResult validateAll(SubmissionEvent event) {
        log.info("Starting validation orchestration for submission {}", event.getSubmissionId());

        // Sort validators by order
        List<SubmissionValidator> sortedValidators = getValidators();

        log.debug("Executing {} validators in order", sortedValidators.size());

        for (SubmissionValidator validator : sortedValidators) {
            log.debug("Running validator: {} (order: {})",
                    validator.getValidatorName(), validator.getOrder());

            ValidationResult result = validator.validate(event);

            if (!result.isValid()) {
                log.warn("Validation failed at {}: {}",
                        validator.getValidatorName(), result.getErrorMessage());
                return result;
            }

            log.debug("Validator {} passed", validator.getValidatorName());
        }

        log.info("All validations passed for submission {}", event.getSubmissionId());
        return ValidationResult.success("ValidationOrchestrator");
    }

    /**
     * Get list of all registered validators.
     *
     * @return List of validators sorted by order
     */
    public List<SubmissionValidator> getValidators() {
        return validators.stream()
                .sorted(Comparator.comparingDouble(SubmissionValidator::getOrder))
                .toList();
    }

    /**
     * Log information about registered validators.
     */
    public void logValidatorInfo() {
        log.info("Registered validators ({}):", validators.size());
        validators.stream()
                .sorted(Comparator.comparingDouble(SubmissionValidator::getOrder))
                .forEach(v -> log.info("  - {} (order: {})", v.getValidatorName(), v.getOrder()));
    }
}

