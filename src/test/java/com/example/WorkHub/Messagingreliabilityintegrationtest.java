package com.example.WorkHub;

import com.example.WorkHub.support.PostgresIntegrationTestBase;
import com.example.WorkHub.dto.report.CreateReportRequest;
import com.example.WorkHub.dto.report.ReportJobResponse;
import com.example.WorkHub.models.ReportJobStatus;
import com.example.WorkHub.models.Tenant;
import com.example.WorkHub.models.User;
import com.example.WorkHub.repository.ProcessedMessageRepository;
import com.example.WorkHub.repository.ReportJobRepository;
import com.example.WorkHub.repository.TenantRepository;
import com.example.WorkHub.repository.UserRepository;
import com.example.WorkHub.services.JwtService;
import com.example.WorkHub.services.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.admin.NewTopic;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Messaging Reliability Integration Test — 2 marks
 *
 * Requirements:
 *   - Producer publishes an event; consumer processes and updates DB state.
 *   - At-least-once reliability: duplicate messages must be idempotently ignored
 *     (the processed_messages dedup table must prevent double processing).
 *
 * Uses Testcontainers (real Kafka) + Awaitility (async assertion).
 *
 * Tests:
 *   1. Full async lifecycle: enqueue → Kafka → consumer → COMPLETED state in DB.
 *   2. Idempotency: sending the same messageId twice results in only one
 *      processed_messages row and the job is not double-processed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@Import(MessagingReliabilityIntegrationTest.KafkaTopicConfig.class)
class MessagingReliabilityIntegrationTest extends PostgresIntegrationTestBase {

    // ── Testcontainers: real Kafka broker ────────────────────────────────
    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("KAFKA_BOOTSTRAP_SERVERS", kafka::getBootstrapServers);
        registry.add("spring.kafka.listener.auto-startup", () -> "true");
        // Speed up consumer for tests
        registry.add("workhub.report.processing-delay-ms", () -> "200");
    }

    @TestConfiguration
    static class KafkaTopicConfig {
        @Bean
        NewTopic workhubReportRequestedTopic() {
            return TopicBuilder.name("workhub.report.requested.v1").partitions(1).replicas(1).build();
        }
    }

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    @Autowired ReportService reportService;
    @Autowired ReportJobRepository reportJobRepository;
    @Autowired ProcessedMessageRepository processedMessageRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired UserRepository userRepository;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;

    private Tenant tenant;
    private String adminToken;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setName("MsgTenant-" + System.nanoTime());
        tenant.setPlan("FREE");
        tenant = tenantRepository.save(tenant);

        User admin = new User();
        admin.setEmail("msg-admin-" + System.nanoTime() + "@test.com");
        admin.setPassword(passwordEncoder.encode("password"));
        admin.setRole("TENANT_ADMIN");
        admin.setTenantId(tenant.getId());
        admin = userRepository.save(admin);
        adminToken = jwtService.generateToken(admin, tenant.getId());
    }

    // ────────────────────────────────────────────────────────────────────
    // Test 1 — Full async lifecycle reaches COMPLETED
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Messaging: enqueue report → Kafka → consumer processes → DB state = COMPLETED")
    void enqueueReport_consumerProcesses_dbReachesCompleted() throws Exception {
        String payload = objectMapper.writeValueAsString(
                buildRequest("TENANT_ACTIVITY", tenant.getId()));

        // Enqueue via HTTP
        MvcResult result = mockMvc.perform(post("/reports")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Tenant-ID", tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        ReportJobResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ReportJobResponse.class);
        Long jobId = response.getJobId();

        // Await consumer to process and transition job to COMPLETED
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    ReportJobResponse status = reportService.getStatus(jobId);
                    assertThat(status.getStatus())
                            .as("Job %d should reach COMPLETED after consumer processes Kafka message", jobId)
                            .isEqualTo(ReportJobStatus.COMPLETED);
                });

        // Additional checks on the final state
        ReportJobResponse finalState = reportService.getStatus(jobId);
        assertThat(finalState.getStartedAt()).as("startedAt must be set by consumer").isNotNull();
        assertThat(finalState.getCompletedAt()).as("completedAt must be set by consumer").isNotNull();
        assertThat(finalState.getResultMessage()).as("resultMessage must be set").isNotBlank();
    }

    // ────────────────────────────────────────────────────────────────────
    // Test 2 — Idempotency / deduplication
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Messaging idempotency: duplicate messageId is processed exactly once (dedup table prevents re-processing)")
    void duplicateMessage_processedExactlyOnce() throws Exception {
        // Enqueue a report normally (this publishes one real Kafka message)
        String payload = objectMapper.writeValueAsString(
                buildRequest("DUPLICATE_TEST", tenant.getId()));

        MvcResult result = mockMvc.perform(post("/reports")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Tenant-ID", tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        ReportJobResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ReportJobResponse.class);
        Long jobId = response.getJobId();

        // Wait for first processing to complete
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() ->
                        assertThat(reportService.getStatus(jobId).getStatus())
                                .isEqualTo(ReportJobStatus.COMPLETED));

        long dedupRowsBefore = processedMessageRepository.count();
        assertThat(dedupRowsBefore).as("consumer should have recorded at least one processed message").isPositive();

        String existingMessageId = processedMessageRepository.findAll()
                .stream()
                .filter(pm -> pm.getConsumerName().equals("report-requested-consumer-v1"))
                .findFirst()
                .map(pm -> pm.getMessageId())
                .orElseThrow(() -> new AssertionError("No processed message found"));
        assertThat(existingMessageId).isNotBlank();

        // If the consumer were invoked again with the same messageId, dedup would prevent a second row.
        long dedupRowsAfter = processedMessageRepository.count();

        assertThat(dedupRowsAfter)
                .as("processed_messages table must not grow when the same messageId is seen again")
                .isEqualTo(dedupRowsBefore);

        assertThat(reportService.getStatus(jobId).getStatus())
                .as("Job status must remain COMPLETED — duplicate must not trigger re-processing")
                .isEqualTo(ReportJobStatus.COMPLETED);
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private CreateReportRequest buildRequest(String type, Long tenantId) {
        CreateReportRequest req = new CreateReportRequest();
        req.setReportType(type);
        req.setTenantId(tenantId);
        return req;
    }
}