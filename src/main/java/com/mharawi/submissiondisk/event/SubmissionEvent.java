package com.mharawi.submissiondisk.event;

import com.mharawi.submissiondisk.entity.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionEvent {
    private Long submissionId;
    private String fileName;
    private String originalFileName;
    private Long fileSize;
    private String contentType;
    private String storagePath;
    private String description;
    private String submittedBy;
    private SubmissionStatus status;
    private String checksum;
    private LocalDateTime timestamp;
    private String errorMessage;
    
    // Pipeline stage tracking
    private String currentStage;
    private String nextStage;
}

