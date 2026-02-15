package com.mharawi.submissiondisk.repository;

import com.mharawi.submissiondisk.entity.Submission;
import com.mharawi.submissiondisk.entity.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByStatus(SubmissionStatus status);

    List<Submission> findBySubmittedBy(String submittedBy);

    List<Submission> findBySubmittedAtBetween(LocalDateTime start, LocalDateTime end);

    Optional<Submission> findByChecksum(String checksum);

    @Query("SELECT s FROM Submission s WHERE s.status = ?1 ORDER BY s.submittedAt DESC")
    List<Submission> findByStatusOrderBySubmittedAtDesc(SubmissionStatus status);

    @Query("SELECT s FROM Submission s WHERE s.submittedBy = ?1 AND s.status = ?2")
    List<Submission> findBySubmittedByAndStatus(String submittedBy, SubmissionStatus status);
}

