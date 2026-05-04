package com.example.WorkHub.repository;

import com.example.WorkHub.models.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, Long> {
    boolean existsByMessageIdAndConsumerName(String messageId, String consumerName);
}
