package com.example.WorkHub.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to enqueue an asynchronous report job")
public class CreateReportRequest {
    @NotBlank(message = "Report type is required")
    @Size(max = 120, message = "Report type must not exceed 120 characters")
    @Schema(description = "Logical report type", example = "TENANT_ACTIVITY")
    private String reportType;

    @NotNull(message = "Tenant ID is required")
    @Schema(description = "Tenant identifier that owns this report", example = "1")
    private Long tenantId;

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
