package com.example.WorkHub.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "WorkHub API",
                version = "1.0",
                description = """
                        WorkHub REST API — multi-tenant workspace management.

                        **Data Integrity highlight:** The `POST /projects` endpoint creates a Project
                        and its initial Task in a single atomic transaction. Use `simulateTaskFailure = true`
                        to trigger a rollback and prove neither row is committed."""
        )
)
public class OpenApiConfig {
}
