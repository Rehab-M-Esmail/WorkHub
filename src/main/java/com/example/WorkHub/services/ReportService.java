package com.example.WorkHub.services;

import com.example.WorkHub.dto.report.CreateReportRequest;
import com.example.WorkHub.dto.report.ReportJobResponse;
import com.example.WorkHub.exception.ResourceNotFoundException;
import com.example.WorkHub.messaging.event.ReportRequestedEvent;
import com.example.WorkHub.messaging.producer.ReportEventProducer;
import com.example.WorkHub.models.ReportJob;
import com.example.WorkHub.models.ReportJobStatus;
import com.example.WorkHub.repository.ReportJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ReportService {
    private final ReportJobRepository reportJobRepository;
    private final ReportEventProducer reportEventProducer;

    public ReportService(ReportJobRepository reportJobRepository, ReportEventProducer reportEventProducer) {
        this.reportJobRepository = reportJobRepository;
        this.reportEventProducer = reportEventProducer;
    }

    @Transactional
    public ReportJobResponse enqueueReport(CreateReportRequest request) {
        ReportJob reportJob = new ReportJob();
        reportJob.setReportType(request.getReportType());
        reportJob.setTenantId(request.getTenantId());
        reportJob.setStatus(ReportJobStatus.QUEUED);
        reportJob = reportJobRepository.save(reportJob);

        ReportRequestedEvent event = new ReportRequestedEvent(
                UUID.randomUUID().toString(),
                reportJob.getId(),
                reportJob.getReportType(),
                reportJob.getTenantId()
        );
        reportEventProducer.publish(event);
        return toResponse(reportJob);
    }

    @Transactional(readOnly = true)
    public ReportJobResponse getStatus(Long jobId) {
        ReportJob reportJob = reportJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Report job not found with id: " + jobId));
        return toResponse(reportJob);
    }

    @Transactional
    public void markProcessing(Long jobId) {
        ReportJob reportJob = findJob(jobId);
        reportJob.setStatus(ReportJobStatus.PROCESSING);
        reportJob.setStartedAt(Instant.now());
        reportJob.setFailureReason(null);
    }

    @Transactional
    public void markCompleted(Long jobId, String resultMessage) {
        ReportJob reportJob = findJob(jobId);
        reportJob.setStatus(ReportJobStatus.COMPLETED);
        reportJob.setCompletedAt(Instant.now());
        reportJob.setResultMessage(resultMessage);
        reportJob.setFailureReason(null);
    }

    @Transactional
    public void markFailed(Long jobId, String reason) {
        ReportJob reportJob = findJob(jobId);
        reportJob.setStatus(ReportJobStatus.FAILED);
        reportJob.setCompletedAt(Instant.now());
        reportJob.setFailureReason(reason);
    }

    private ReportJob findJob(Long jobId) {
        return reportJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Report job not found with id: " + jobId));
    }

    private ReportJobResponse toResponse(ReportJob reportJob) {
        return new ReportJobResponse(
                reportJob.getId(),
                reportJob.getReportType(),
                reportJob.getTenantId(),
                reportJob.getStatus(),
                reportJob.getResultMessage(),
                reportJob.getFailureReason(),
                reportJob.getCreatedAt(),
                reportJob.getStartedAt(),
                reportJob.getCompletedAt()
        );
    }
}
