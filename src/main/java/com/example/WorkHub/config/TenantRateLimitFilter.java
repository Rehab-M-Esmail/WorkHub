package com.example.WorkHub.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TenantRateLimitFilter extends OncePerRequestFilter {

    // Thread-safe map to store a unique token bucket per tenant
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    // Free? 10, Premium 100 per minute
    private Bucket createNewBucket(String plan) {
        if ("PREMIUM".equalsIgnoreCase(plan)) {
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
                    .build();
        }
        return Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1))))
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extract Tenant ID and Plan
        String tenantId = (String) request.getAttribute("X-Tenant-ID");
        String tenantPlan = (String) request.getAttribute("X-Tenant-Plan");

        // Fallback to anonymous tier if no tenant context is available (e.g., public endpoints)
        if (tenantId == null) {
            tenantId = "anonymous";
            tenantPlan = "FREE";
        }

        // create the bucket for this specific tenant
        String tenantKey = tenantId;
        String plan = tenantPlan;
        Bucket bucket = cache.computeIfAbsent(tenantKey, k -> createNewBucket(plan));

        // 3. Try to consume 1 token from the tenant's bucket
        if (bucket.tryConsume(1)) {
            // Remaining tokens left
            response.addHeader("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            filterChain.doFilter(request, response);
        } else {
            // Bucket empty: Throttled!
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 429 Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Too many requests. Your tenant plan limits have been exceeded.\"}");
        }
    }
}