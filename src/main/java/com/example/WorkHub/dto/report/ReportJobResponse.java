package com.example.WorkHub.dto.report;

import com.example.WorkHub.models.ReportJobStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Report job lifecycle view")
public class ReportJobResponse {
    @Schema(example = "10")
    private Long jobId;

    @Schema(example = "TENANT_ACTIVITY")
    private String reportType;

    @Schema(example = "1")
    private Long tenantId;

    @Schema(example = "QUEUED")
    private ReportJobStatus status;

    @Schema(example = "Report generated successfully")
    private String resultMessage;

    @Schema(example = "null")
    private String failureReason;

    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;

    @JsonCreator
    public ReportJobResponse(
            @JsonProperty("jobId") Long jobId,
            @JsonProperty("reportType") String reportType,
            @JsonProperty("tenantId") Long tenantId,
            @JsonProperty("status") ReportJobStatus status,
            @JsonProperty("resultMessage") String resultMessage,
            @JsonProperty("failureReason") String failureReason,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("startedAt") Instant startedAt,
            @JsonProperty("completedAt") Instant completedAt) {
        this.jobId = jobId;
        this.reportType = reportType;
        this.tenantId = tenantId;
        this.status = status;
        this.resultMessage = resultMessage;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getReportType() {
        return reportType;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public ReportJobStatus getStatus() {
        return status;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
