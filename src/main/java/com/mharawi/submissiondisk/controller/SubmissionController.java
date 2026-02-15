package com.mharawi.submissiondisk.controller;

import com.mharawi.submissiondisk.dto.SubmissionResponse;
import com.mharawi.submissiondisk.entity.SubmissionStatus;
import com.mharawi.submissiondisk.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@Slf4j
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    public ResponseEntity<SubmissionResponse> uploadSubmission(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "submittedBy", required = false) String submittedBy) {

        log.info("Received submission upload request: {}", file.getOriginalFilename());

        try {
            SubmissionResponse response = submissionService.createSubmission(file, description, submittedBy);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid submission: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionResponse> getSubmission(@PathVariable Long id) {
        return submissionService.getSubmissionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<SubmissionResponse>> getAllSubmissions(
            @RequestParam(value = "status", required = false) SubmissionStatus status,
            @RequestParam(value = "submittedBy", required = false) String submittedBy) {

        List<SubmissionResponse> submissions;

        if (status != null) {
            submissions = submissionService.getSubmissionsByStatus(status);
        } else if (submittedBy != null) {
            submissions = submissionService.getSubmissionsByUser(submittedBy);
        } else {
            submissions = submissionService.getAllSubmissions();
        }

        return ResponseEntity.ok(submissions);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateSubmissionStatus(
            @PathVariable Long id,
            @RequestParam SubmissionStatus status) {

        submissionService.updateSubmissionStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubmission(@PathVariable Long id) {
        submissionService.deleteSubmission(id);
        return ResponseEntity.noContent().build();
    }
}

