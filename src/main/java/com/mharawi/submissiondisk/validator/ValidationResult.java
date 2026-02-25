package com.mharawi.submissiondisk.validator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a validation operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    private boolean valid;
    private String errorMessage;
    private String validatorName;

    /**
     * Create a successful validation result.
     */
    public static ValidationResult success(String validatorName) {
        return ValidationResult.builder()
                .valid(true)
                .validatorName(validatorName)
                .build();
    }

    /**
     * Create a failed validation result with error message.
     */
    public static ValidationResult failure(String validatorName, String errorMessage) {
        return ValidationResult.builder()
                .valid(false)
                .validatorName(validatorName)
                .errorMessage(errorMessage)
                .build();
    }
}

