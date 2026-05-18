package com.example.WorkHub.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a shared Postgres container for integration tests when no external datasource URL is
 * provided (local {@code mvn test}). CI sets {@code SPRING_DATASOURCE_URL} so the service Postgres
 * is used instead.
 */
public final class PostgresTestContainer {

    private static final DockerImageName IMAGE = DockerImageName.parse("postgres:16-alpine");

    private static volatile PostgreSQLContainer<?> container;

    private PostgresTestContainer() {}

    public static void registerProperties(DynamicPropertyRegistry registry) {
        if (useExternalDatasource()) {
            return;
        }
        startIfNeeded();
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
    }

    private static boolean useExternalDatasource() {
        String url = System.getenv("SPRING_DATASOURCE_URL");
        return url != null && !url.isBlank();
    }

    private static void startIfNeeded() {
        if (container == null) {
            synchronized (PostgresTestContainer.class) {
                if (container == null) {
                    container = new PostgreSQLContainer<>(IMAGE)
                            .withDatabaseName("workhub")
                            .withUsername("postgres")
                            .withPassword("postgres");
                    container.start();
                }
            }
        }
    }
}
