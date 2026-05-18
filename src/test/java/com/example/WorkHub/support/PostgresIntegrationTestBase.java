package com.example.WorkHub.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Shared {@link DynamicPropertySource} for Spring Boot integration tests that need Postgres.
 */
public abstract class PostgresIntegrationTestBase {

    @DynamicPropertySource
    static void registerPostgres(DynamicPropertyRegistry registry) {
        PostgresTestContainer.registerProperties(registry);
    }
}
