package com.example.WorkHub.messaging.event;

public class ReportRequestedEvent {
    private String messageId;
    private Long jobId;
    private String reportType;
    private Long tenantId;

    public ReportRequestedEvent() {
    }

    public ReportRequestedEvent(String messageId, Long jobId, String reportType, Long tenantId) {
        this.messageId = messageId;
        this.jobId = jobId;
        this.reportType = reportType;
        this.tenantId = tenantId;
    }

    public String getMessageId() {
        return messageId;
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
}
