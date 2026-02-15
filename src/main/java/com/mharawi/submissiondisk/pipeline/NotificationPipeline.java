package com.mharawi.submissiondisk.pipeline;

import com.mharawi.submissiondisk.config.KafkaTopicConfig;
import com.mharawi.submissiondisk.event.SubmissionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPipeline {

    @KafkaListener(topics = KafkaTopicConfig.SUBMISSION_NOTIFICATION, groupId = "${spring.kafka.consumer.group-id}")
    public void sendNotification(SubmissionEvent event) {
        log.info("NOTIFICATION PIPELINE: Sending notification for submission ID: {}", event.getSubmissionId());

        try {
            // This pipeline can be extended to:
            // - Send email notifications
            // - Send webhook callbacks
            // - Update external systems
            // - Send Slack/Teams messages
            // - Push notifications to mobile apps

            log.info("NOTIFICATION PIPELINE: Notification sent for submission {} to user {}",
                    event.getSubmissionId(), event.getSubmittedBy());
            log.info("NOTIFICATION PIPELINE: Status: {}, Stage: {}",
                    event.getStatus(), event.getCurrentStage());

        } catch (Exception e) {
            log.error("NOTIFICATION PIPELINE: Error sending notification for submission {}",
                    event.getSubmissionId(), e);
        }
    }

    @KafkaListener(topics = KafkaTopicConfig.SUBMISSION_COMPLETED, groupId = "${spring.kafka.consumer.group-id}")
    public void handleCompleted(SubmissionEvent event) {
        log.info("NOTIFICATION PIPELINE: Submission {} completed successfully", event.getSubmissionId());
        // Handle completed submissions
    }

    @KafkaListener(topics = KafkaTopicConfig.SUBMISSION_FAILED, groupId = "${spring.kafka.consumer.group-id}")
    public void handleFailed(SubmissionEvent event) {
        log.error("NOTIFICATION PIPELINE: Submission {} failed at stage {} with error: {}",
                event.getSubmissionId(), event.getCurrentStage(), event.getErrorMessage());
        // Handle failed submissions - send alerts, log to monitoring system, etc.
    }
}

