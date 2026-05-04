package com.example.WorkHub.controllers;

import com.example.WorkHub.dto.report.CreateReportRequest;
import com.example.WorkHub.dto.report.ReportJobResponse;
import com.example.WorkHub.models.ApiError;
import com.example.WorkHub.services.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@Tag(name = "Reports", description = "Asynchronous report workflow endpoints")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(
            summary = "Enqueue report job",
            description = "Creates a report job in QUEUED status and publishes a Kafka message for background processing.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Report job queued"),
                    @ApiResponse(responseCode = "400", description = "Validation failed",
                            content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    @PostMapping
    public ResponseEntity<ReportJobResponse> enqueueReport(@Valid @RequestBody CreateReportRequest request) {
        ReportJobResponse response = reportService.enqueueReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get report status",
            description = "Returns the current state of an asynchronous report job.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Report job found"),
                    @ApiResponse(responseCode = "404", description = "Report job not found",
                            content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    @GetMapping("/{jobId}/status")
    public ResponseEntity<ReportJobResponse> getReportStatus(@PathVariable Long jobId) {
        return ResponseEntity.ok(reportService.getStatus(jobId));
    }
}
