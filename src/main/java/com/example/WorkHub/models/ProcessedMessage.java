package com.example.WorkHub.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "processed_messages",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"message_id", "consumer_name"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ProcessedMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, length = 100)
    private String messageId;

    @Column(name = "consumer_name", nullable = false, length = 100)
    private String consumerName;

    @Column(nullable = false, updatable = false)
    private Instant processedAt;

    @PrePersist
    void onCreate() {
        this.processedAt = Instant.now();
    }
}
