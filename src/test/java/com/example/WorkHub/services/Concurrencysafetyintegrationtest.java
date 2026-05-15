package com.example.WorkHub.services;

import com.example.WorkHub.config.multitenancy.TenantContext;
import com.example.WorkHub.models.*;
import com.example.WorkHub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class ConcurrencySafetyIntegrationTest {

    @Autowired TaskRepository taskRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private static final int THREAD_COUNT = 8;

    private Long taskId;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setName("ConcTenant-" + System.nanoTime());
        tenant.setPlan("FREE");
        tenant = tenantRepository.save(tenant);

        TenantContext.setTenantId(tenant.getId());
        try {
            Project project = new Project();
            project.setName("ConcProject-" + System.nanoTime());
            project.setTenantId(tenant.getId());
            project = projectRepository.save(project);

            Task task = new Task();
            task.setTitle("Concurrent Task");
            task.setStatus("TODO");
            task.setProject(project);
            task.setTenantId(tenant.getId());
            task = taskRepository.save(task);
            taskId = task.getId();
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Concurrency: N concurrent updates to same Task — optimistic lock prevents lost updates")
    void concurrentUpdates_optimisticLockingPreventsLostUpdates() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startGate = new CountDownLatch(1);   // all threads start simultaneously
        CountDownLatch doneLatch  = new CountDownLatch(THREAD_COUNT);

        AtomicInteger successCount  = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < THREAD_COUNT; i++) {
            final String newStatus = "STATUS_" + i;
            executor.submit(() -> {
                try {
                    startGate.await();   // all threads released at the same instant
                    TenantContext.setTenantId(tenant.getId());
                    try {
                        // Each thread reads the same version, then tries to write
                        Task t = taskRepository.findById(taskId)
                                .orElseThrow(() -> new IllegalStateException("Task not found"));
                        t.setStatus(newStatus);
                        taskRepository.saveAndFlush(t);
                        successCount.incrementAndGet();
                    } finally {
                        TenantContext.clear();
                    }
                } catch (ObjectOptimisticLockingFailureException e) {
                    conflictCount.incrementAndGet();   // expected: losing threads get this
                } catch (Exception e) {
                    unexpectedErrors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown();                    // release all threads
        doneLatch.await(30, TimeUnit.SECONDS);    // wait for all to finish
        executor.shutdown();

        // ── Assertions ──────────────────────────────────────────────────

        assertThat(unexpectedErrors)
                .as("No unexpected errors should occur — only OptimisticLockingFailure is expected")
                .isEmpty();

        assertThat(successCount.get())
                .as("At least one thread must succeed")
                .isGreaterThanOrEqualTo(1);

        assertThat(conflictCount.get())
                .as("At least one thread must be rejected with an optimistic lock conflict — proving no silent lost update")
                .isGreaterThanOrEqualTo(1);

        assertThat(successCount.get() + conflictCount.get())
                .as("All threads must either succeed or conflict — none should silently disappear")
                .isEqualTo(THREAD_COUNT);

        // ── Final state check: task exists and has a committed status ────
        TenantContext.setTenantId(tenant.getId());
        try {
            Task finalTask = taskRepository.findById(taskId).orElseThrow();
            assertThat(finalTask.getStatus())
                    .as("Final task status must be one of the submitted statuses (not original 'TODO' unless a thread kept it)")
                    .startsWith("STATUS_");
        } finally {
            TenantContext.clear();
        }
    }
}