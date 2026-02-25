package com.mharawi.submissiondisk.validator;

import com.mharawi.submissiondisk.event.SubmissionEvent;

/**
 * Interface for all submission validators.
 * Each validator implements a specific validation rule.
 */
public interface SubmissionValidator {

    /**
     * Validate a submission event.
     *
     * @param event The submission event to validate
     * @return ValidationResult containing success/failure and error message
     */
    ValidationResult validate(SubmissionEvent event);

    /**
     * Get the name of this validator for logging purposes.
     *
     * @return Validator name
     */
    String getValidatorName();

    /**
     * Get the order in which this validator should run.
     * Lower numbers run first.
     *
     * Use decimal values (e.g., 10.5, 20.5) to insert between existing validators
     * without modifying their order values.
     *
     * @return Order priority (default 100)
     */
    default double getOrder() {
        return 100;
    }
}

