package com.mharawi.submissiondisk.pipeline;

import com.mharawi.submissiondisk.config.KafkaTopicConfig;
import com.mharawi.submissiondisk.entity.SubmissionStatus;
import com.mharawi.submissiondisk.event.SubmissionEvent;
import com.mharawi.submissiondisk.repository.SubmissionRepository;
import com.mharawi.submissiondisk.validator.ValidationOrchestrator;
import com.mharawi.submissiondisk.validator.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationPipeline {

    private final SubmissionRepository submissionRepository;
    private final SubmissionProducer submissionProducer;
    private final ValidationOrchestrator validationOrchestrator;

    @PostConstruct
    public void init() {
        validationOrchestrator.logValidatorInfo();
    }

    @KafkaListener(topics = KafkaTopicConfig.SUBMISSION_VALIDATION, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void validateSubmission(SubmissionEvent event) {
        log.info("VALIDATION PIPELINE: Processing submission ID: {}", event.getSubmissionId());

        try {
            // Execute all validators through orchestrator
            ValidationResult result = validationOrchestrator.validateAll(event);

            if (!result.isValid()) {
                log.warn("VALIDATION PIPELINE: Validation failed for submission {}: {} - {}",
                        event.getSubmissionId(), result.getValidatorName(), result.getErrorMessage());
                handleValidationFailure(event, result.getErrorMessage());
                return;
            }

            log.info("VALIDATION PIPELINE: Submission {} validated successfully", event.getSubmissionId());
            event.setStatus(SubmissionStatus.VALIDATED);
            event.setCurrentStage("VALIDATION");
            event.setNextStage("STORAGE");

            submissionRepository.findById(event.getSubmissionId()).ifPresent(submission -> {
                submission.setStatus(SubmissionStatus.VALIDATED);
                submissionRepository.save(submission);
            });

            submissionProducer.sendEvent(KafkaTopicConfig.SUBMISSION_STORAGE, event);
        } catch (Exception e) {
            log.error("VALIDATION PIPELINE: Error validating submission {}", event.getSubmissionId(), e);
            handleValidationFailure(event, "Validation error: " + e.getMessage());
        }
    }

    private void handleValidationFailure(SubmissionEvent event, String errorMessage) {
        event.setStatus(SubmissionStatus.FAILED);
        event.setErrorMessage(errorMessage);
        event.setCurrentStage("VALIDATION");
        event.setNextStage("FAILED");

        submissionRepository.findById(event.getSubmissionId()).ifPresent(submission -> {
            submission.setStatus(SubmissionStatus.FAILED);
            submissionRepository.save(submission);
        });

        submissionProducer.sendEvent(KafkaTopicConfig.SUBMISSION_FAILED, event);
    }
}

