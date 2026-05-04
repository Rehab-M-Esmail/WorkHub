package com.example.WorkHub.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "report_jobs")
@Getter
@Setter
@NoArgsConstructor
public class ReportJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String reportType;

    @Column(nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportJobStatus status;

    @Column(length = 1000)
    private String resultMessage;

    @Column(length = 2000)
    private String failureReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant startedAt;

    @Column
    private Instant completedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
