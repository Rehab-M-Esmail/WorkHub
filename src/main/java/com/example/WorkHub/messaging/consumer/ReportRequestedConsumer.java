package com.example.WorkHub.messaging.consumer;

import com.example.WorkHub.messaging.event.ReportRequestedEvent;
import com.example.WorkHub.models.ProcessedMessage;
import com.example.WorkHub.repository.ProcessedMessageRepository;
import com.example.WorkHub.services.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReportRequestedConsumer {
    private static final Logger log = LoggerFactory.getLogger(ReportRequestedConsumer.class);
    private static final String CONSUMER_NAME = "report-requested-consumer-v1";

    private final ProcessedMessageRepository processedMessageRepository;
    private final ReportService reportService;
    private final long processingDelayMs;

    public ReportRequestedConsumer(
            ProcessedMessageRepository processedMessageRepository,
            ReportService reportService,
            @Value("${workhub.report.processing-delay-ms:1500}") long processingDelayMs) {
        this.processedMessageRepository = processedMessageRepository;
        this.reportService = reportService;
        this.processingDelayMs = processingDelayMs;
    }

    @KafkaListener(topics = "${workhub.kafka.topics.report-requested}")
    @Transactional
    public void consume(ReportRequestedEvent event) {
        if (event == null || event.getMessageId() == null) {
            log.warn("Ignoring invalid report event: {}", event);
            return;
        }

        if (processedMessageRepository.existsByMessageIdAndConsumerName(event.getMessageId(), CONSUMER_NAME)) {
            log.info("Skipping duplicate report event messageId={} jobId={}",
                    event.getMessageId(), event.getJobId());
            return;
        }

        try {
            ProcessedMessage processedMessage = new ProcessedMessage();
            processedMessage.setMessageId(event.getMessageId());
            processedMessage.setConsumerName(CONSUMER_NAME);
            processedMessageRepository.saveAndFlush(processedMessage);
        } catch (DataIntegrityViolationException ex) {
            log.info("Duplicate insert detected, skipping messageId={} jobId={}",
                    event.getMessageId(), event.getJobId());
            return;
        }

        try {
            reportService.markProcessing(event.getJobId());
            Thread.sleep(processingDelayMs);
            reportService.markCompleted(event.getJobId(),
                    "Report " + event.getReportType() + " generated for tenant " + event.getTenantId());
            log.info("Processed report event messageId={} jobId={}", event.getMessageId(), event.getJobId());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            reportService.markFailed(event.getJobId(), "Processing interrupted");
            throw new IllegalStateException("Report processing interrupted", ex);
        } catch (Exception ex) {
            reportService.markFailed(event.getJobId(), ex.getMessage());
            throw ex;
        }
    }
}
