package com.mharawi.submissiondisk.pipeline;

import com.mharawi.submissiondisk.config.KafkaTopicConfig;
import com.mharawi.submissiondisk.entity.Submission;
import com.mharawi.submissiondisk.entity.SubmissionStatus;
import com.mharawi.submissiondisk.event.SubmissionEvent;
import com.mharawi.submissiondisk.repository.SubmissionRepository;
import com.mharawi.submissiondisk.service.VirusScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.capybara.clamav.commands.scan.result.ScanResult;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationPipeline {

    private final SubmissionRepository submissionRepository;
    private final SubmissionProducer submissionProducer;
    private final VirusScanService virusScanService;

    @KafkaListener(topics = KafkaTopicConfig.SUBMISSION_VALIDATION, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void validateSubmission(SubmissionEvent event) {
        log.info("VALIDATION PIPELINE: Processing submission ID: {}", event.getSubmissionId());

        try {
            Optional<Submission> existingSubmission = submissionRepository.findByChecksum(event.getChecksum());

            if (existingSubmission.isPresent() && !existingSubmission.get().getId().equals(event.getSubmissionId())) {
                log.warn("VALIDATION PIPELINE: Duplicate submission detected with checksum: {}", event.getChecksum());
                handleValidationFailure(event, "This file has already been submitted");
                return;
            }

            log.info("VALIDATION PIPELINE: Starting virus scan for submission {}", event.getSubmissionId());
            try {
                ScanResult scanResult = virusScanService.scanFile(event.getStoragePath());

                if (scanResult == null) {
                    log.info("VALIDATION PIPELINE: Virus scan skipped (ClamAV disabled) for submission {}", event.getSubmissionId());
                } else if (scanResult instanceof ScanResult.VirusFound virusFound) {
                    log.error("VALIDATION PIPELINE: VIRUS DETECTED in submission {}: {}", event.getSubmissionId(), virusFound);
                    handleValidationFailure(event, "Virus detected: " + virusFound);
                    return;
                } else if (!(scanResult instanceof ScanResult.OK)) {
                    log.warn("VALIDATION PIPELINE: Unexpected scan result for submission {}: {}", event.getSubmissionId(), scanResult);
                } else {
                    log.info("VALIDATION PIPELINE: Virus scan PASSED for submission {}", event.getSubmissionId());
                }
            } catch (Exception scanException) {
                log.error("VALIDATION PIPELINE: Error during virus scan for submission {}", event.getSubmissionId(), scanException);
                handleValidationFailure(event, "Virus scan failed: " + scanException.getMessage());
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

