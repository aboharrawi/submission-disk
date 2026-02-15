package com.mharawi.submissiondisk.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String SUBMISSION_RECEIVED = "submission.received";
    public static final String SUBMISSION_VALIDATION = "submission.validation";
    public static final String SUBMISSION_STORAGE = "submission.storage";
    public static final String SUBMISSION_PROCESSING = "submission.processing";
    public static final String SUBMISSION_NOTIFICATION = "submission.notification";
    public static final String SUBMISSION_FAILED = "submission.failed";
    public static final String SUBMISSION_COMPLETED = "submission.completed";

    @Bean
    public NewTopic submissionReceivedTopic() {
        return TopicBuilder.name(SUBMISSION_RECEIVED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic submissionValidationTopic() {
        return TopicBuilder.name(SUBMISSION_VALIDATION)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic submissionStorageTopic() {
        return TopicBuilder.name(SUBMISSION_STORAGE)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic submissionProcessingTopic() {
        return TopicBuilder.name(SUBMISSION_PROCESSING)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic submissionNotificationTopic() {
        return TopicBuilder.name(SUBMISSION_NOTIFICATION)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic submissionFailedTopic() {
        return TopicBuilder.name(SUBMISSION_FAILED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic submissionCompletedTopic() {
        return TopicBuilder.name(SUBMISSION_COMPLETED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}

