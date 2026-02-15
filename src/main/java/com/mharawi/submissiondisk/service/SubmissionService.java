package com.mharawi.submissiondisk.service;

import com.mharawi.submissiondisk.config.KafkaTopicConfig;
import com.mharawi.submissiondisk.dto.SubmissionResponse;
import com.mharawi.submissiondisk.entity.Submission;
import com.mharawi.submissiondisk.entity.SubmissionStatus;
import com.mharawi.submissiondisk.event.SubmissionEvent;
import com.mharawi.submissiondisk.pipeline.SubmissionProducer;
import com.mharawi.submissiondisk.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final FileStorageService fileStorageService;
    private final SubmissionProducer submissionProducer;

    @Transactional
    public SubmissionResponse createSubmission(MultipartFile file, String description, String submittedBy) {
        try {
            // Validate file
            if (!fileStorageService.isValidZipFile(file)) {
                throw new IllegalArgumentException("Invalid ZIP file");
            }

            // Calculate checksum
            String checksum = fileStorageService.calculateChecksum(file);

            // Store file immediately (async processing will handle validation)
            String storagePath = fileStorageService.storeFile(file);

            // Create submission entity with PENDING status
            Submission submission = Submission.builder()
                    .fileName(file.getOriginalFilename())
                    .originalFileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .storagePath(storagePath)
                    .description(description)
                    .submittedBy(submittedBy)
                    .status(SubmissionStatus.PENDING)
                    .checksum(checksum)
                    .build();

            Submission savedSubmission = submissionRepository.save(submission);
            log.info("Submission created successfully with ID: {}", savedSubmission.getId());

            // Create event and send to Kafka validation pipeline
            SubmissionEvent event = SubmissionEvent.builder()
                    .submissionId(savedSubmission.getId())
                    .fileName(savedSubmission.getFileName())
                    .originalFileName(savedSubmission.getOriginalFileName())
                    .fileSize(savedSubmission.getFileSize())
                    .contentType(savedSubmission.getContentType())
                    .storagePath(savedSubmission.getStoragePath())
                    .description(savedSubmission.getDescription())
                    .submittedBy(savedSubmission.getSubmittedBy())
                    .status(SubmissionStatus.PENDING)
                    .checksum(savedSubmission.getChecksum())
                    .timestamp(LocalDateTime.now())
                    .currentStage("RECEIVED")
                    .nextStage("VALIDATION")
                    .build();

            // Send to Kafka pipeline for async processing
            submissionProducer.sendEvent(KafkaTopicConfig.SUBMISSION_VALIDATION, event);
            log.info("Submission {} sent to Kafka pipeline for processing", savedSubmission.getId());

            return mapToResponse(savedSubmission);

        } catch (IOException e) {
            log.error("Error storing file", e);
            throw new RuntimeException("Failed to store file", e);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error calculating checksum", e);
            throw new RuntimeException("Failed to calculate checksum", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<SubmissionResponse> getSubmissionById(Long id) {
        return submissionRepository.findById(id)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getAllSubmissions() {
        return submissionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionsByStatus(SubmissionStatus status) {
        return submissionRepository.findByStatusOrderBySubmittedAtDesc(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionsByUser(String submittedBy) {
        return submissionRepository.findBySubmittedBy(submittedBy).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateSubmissionStatus(Long id, SubmissionStatus status) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found"));

        submission.setStatus(status);
        if (status == SubmissionStatus.COMPLETED || status == SubmissionStatus.FAILED) {
            submission.setProcessedAt(LocalDateTime.now());
        }

        submissionRepository.save(submission);
        log.info("Submission {} status updated to {}", id, status);
    }

    @Transactional
    public void deleteSubmission(Long id) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found"));

        try {
            fileStorageService.deleteFile(submission.getStoragePath());
            submissionRepository.delete(submission);
            log.info("Submission {} deleted successfully", id);
        } catch (IOException e) {
            log.error("Error deleting file for submission {}", id, e);
            throw new RuntimeException("Failed to delete submission file", e);
        }
    }

    private SubmissionResponse mapToResponse(Submission submission) {
        return SubmissionResponse.builder()
                .id(submission.getId())
                .fileName(submission.getFileName())
                .originalFileName(submission.getOriginalFileName())
                .fileSize(submission.getFileSize())
                .description(submission.getDescription())
                .submittedBy(submission.getSubmittedBy())
                .status(submission.getStatus())
                .submittedAt(submission.getSubmittedAt())
                .processedAt(submission.getProcessedAt())
                .checksum(submission.getChecksum())
                .build();
    }
}

