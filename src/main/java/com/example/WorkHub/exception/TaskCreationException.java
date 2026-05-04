package com.example.WorkHub.exception;

/**
 * Thrown when task creation fails inside a transactional project+task operation.
 * Because this is a RuntimeException, Spring's @Transactional will trigger
 * an automatic rollback of the entire transaction — including the already-persisted
 * project row.
 */
public class TaskCreationException extends RuntimeException {
    public TaskCreationException(String message) {
        super(message);
    }
}
