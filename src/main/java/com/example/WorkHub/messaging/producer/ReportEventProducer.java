package com.example.WorkHub.messaging.producer;

import com.example.WorkHub.messaging.event.ReportRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReportEventProducer {
    private static final Logger log = LoggerFactory.getLogger(ReportEventProducer.class);

    private final KafkaTemplate<String, ReportRequestedEvent> kafkaTemplate;
    private final String reportRequestedTopic;

    public ReportEventProducer(
            KafkaTemplate<String, ReportRequestedEvent> kafkaTemplate,
            @Value("${workhub.kafka.topics.report-requested}") String reportRequestedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.reportRequestedTopic = reportRequestedTopic;
    }

    public void publish(ReportRequestedEvent event) {
        kafkaTemplate.send(reportRequestedTopic, event.getMessageId(), event);
        log.info("Published report event messageId={} jobId={}", event.getMessageId(), event.getJobId());
    }
}
