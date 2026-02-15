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

@Service
@RequiredArgsConstructor
@Slf4j
public class StoragePipeline {

    private final SubmissionRepository submissionRepository;
    private final SubmissionProducer submissionProducer;

    @KafkaListener(topics = KafkaTopicConfig.SUBMISSION_STORAGE, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void processStorage(SubmissionEvent event) {
        log.info("STORAGE PIPELINE: Processing submission ID: {}", event.getSubmissionId());

        try {
            // File is already stored in the initial upload
            // This pipeline can be extended to:
            // - Move files to long-term storage (S3, MinIO, etc.)
            // - Create backups
            // - Generate thumbnails/previews
            // - Extract metadata

            log.info("STORAGE PIPELINE: Storage confirmed for submission {}", event.getSubmissionId());
            event.setStatus(SubmissionStatus.STORED);
            event.setCurrentStage("STORAGE");
            event.setNextStage("PROCESSING");

            // Update submission status in DB
            submissionRepository.findById(event.getSubmissionId()).ifPresent(submission -> {
                submission.setStatus(SubmissionStatus.STORED);
                submissionRepository.save(submission);
            });

            // Send to processing pipeline
            submissionProducer.sendEvent(KafkaTopicConfig.SUBMISSION_PROCESSING, event);

        } catch (Exception e) {
            log.error("STORAGE PIPELINE: Error processing storage for submission {}", event.getSubmissionId(), e);
            event.setStatus(SubmissionStatus.FAILED);
            event.setErrorMessage("Storage error: " + e.getMessage());
            event.setCurrentStage("STORAGE");
            event.setNextStage("FAILED");

            submissionRepository.findById(event.getSubmissionId()).ifPresent(submission -> {
                submission.setStatus(SubmissionStatus.FAILED);
                submissionRepository.save(submission);
            });

            submissionProducer.sendEvent(KafkaTopicConfig.SUBMISSION_FAILED, event);
        }
    }
}

