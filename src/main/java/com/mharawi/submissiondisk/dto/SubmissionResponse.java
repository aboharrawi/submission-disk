package com.mharawi.submissiondisk.dto;

import com.mharawi.submissiondisk.entity.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionResponse {

    private Long id;
    private String fileName;
    private String originalFileName;
    private Long fileSize;
    private String description;
    private String submittedBy;
    private SubmissionStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime processedAt;
    private String checksum;
}

