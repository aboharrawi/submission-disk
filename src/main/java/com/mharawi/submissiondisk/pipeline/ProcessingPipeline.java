package com.mharawi.submissiondisk.pipeline;

import com.mharawi.submissiondisk.config.KafkaTopicConfig;
import com.mharawi.submissiondisk.entity.SubmissionStatus;
import com.mharawi.submissiondisk.event.SubmissionEvent;
import com.mharawi.submissiondisk.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessingPipeline {

    private final SubmissionRepository submissionRepository;
    private final SubmissionProducer submissionProducer;

    @KafkaListener(topics = KafkaTopicConfig.SUBMISSION_PROCESSING, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void processSubmission(SubmissionEvent event) {
        log.info("PROCESSING PIPELINE: Processing submission ID: {}", event.getSubmissionId());

        try {
            // Simulate processing work
            // This pipeline can be extended to:
            // - Unzip and analyze contents
            // - Run security scans
            // - Validate file structure
            // - Extract and process data
            // - Run automated tests

            log.info("PROCESSING PIPELINE: Processing submission {} contents", event.getSubmissionId());

            // Simulate some processing time
            Thread.sleep(1000);

            log.info("PROCESSING PIPELINE: Submission {} processed successfully", event.getSubmissionId());
            event.setStatus(SubmissionStatus.COMPLETED);
            event.setCurrentStage("PROCESSING");
            event.setNextStage("NOTIFICATION");

            // Update submission status in DB
            submissionRepository.findById(event.getSubmissionId()).ifPresent(submission -> {
                submission.setStatus(SubmissionStatus.COMPLETED);
                submission.setProcessedAt(LocalDateTime.now());
                submissionRepository.save(submission);
            });

            // Send to notification pipeline
            submissionProducer.sendEvent(KafkaTopicConfig.SUBMISSION_NOTIFICATION, event);
            submissionProducer.sendEvent(KafkaTopicConfig.SUBMISSION_COMPLETED, event);

        } catch (Exception e) {
            log.error("PROCESSING PIPELINE: Error processing submission {}", event.getSubmissionId(), e);
            event.setStatus(SubmissionStatus.FAILED);
            event.setErrorMessage("Processing error: " + e.getMessage());
            event.setCurrentStage("PROCESSING");
            event.setNextStage("FAILED");

            submissionRepository.findById(event.getSubmissionId()).ifPresent(submission -> {
                submission.setStatus(SubmissionStatus.FAILED);
                submission.setProcessedAt(LocalDateTime.now());
                submissionRepository.save(submission);
            });

            submissionProducer.sendEvent(KafkaTopicConfig.SUBMISSION_FAILED, event);
        }
    }
}

