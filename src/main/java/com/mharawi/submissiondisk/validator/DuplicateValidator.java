package com.mharawi.submissiondisk.validator;

import com.mharawi.submissiondisk.entity.Submission;
import com.mharawi.submissiondisk.event.SubmissionEvent;
import com.mharawi.submissiondisk.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Validates that a submission is not a duplicate based on checksum.
 * Prevents the same file from being submitted multiple times.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DuplicateValidator implements SubmissionValidator {

    private final SubmissionRepository submissionRepository;

    @Override
    public ValidationResult validate(SubmissionEvent event) {
        log.debug("Checking for duplicate submission with checksum: {}", event.getChecksum());

        Optional<Submission> existingSubmission = submissionRepository.findByChecksum(event.getChecksum());

        if (existingSubmission.isPresent() && !existingSubmission.get().getId().equals(event.getSubmissionId())) {
            log.warn("Duplicate submission detected - checksum: {}, existing submission ID: {}, new submission ID: {}",
                    event.getChecksum(), existingSubmission.get().getId(), event.getSubmissionId());
            return ValidationResult.failure(
                    getValidatorName(),
                    "This file has already been submitted (Submission ID: " + existingSubmission.get().getId() + ")"
            );
        }

        log.debug("No duplicate found for checksum: {}", event.getChecksum());
        return ValidationResult.success(getValidatorName());
    }

    @Override
    public String getValidatorName() {
        return "DuplicateValidator";
    }

    @Override
    public double getOrder() {
        return 10;
    }
}

