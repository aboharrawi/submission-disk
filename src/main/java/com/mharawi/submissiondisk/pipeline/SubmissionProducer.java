package com.mharawi.submissiondisk.pipeline;

import com.mharawi.submissiondisk.event.SubmissionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionProducer {

    private final KafkaTemplate<String, SubmissionEvent> kafkaTemplate;

    public void sendEvent(String topic, SubmissionEvent event) {
        log.info("Sending event to topic {}: submissionId={}, stage={}",
                topic, event.getSubmissionId(), event.getCurrentStage());

        kafkaTemplate.send(topic, String.valueOf(event.getSubmissionId()), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully sent event to topic {}: submissionId={}",
                                topic, event.getSubmissionId());
                    } else {
                        log.error("Failed to send event to topic {}: submissionId={}",
                                topic, event.getSubmissionId(), ex);
                    }
                });
    }
}

