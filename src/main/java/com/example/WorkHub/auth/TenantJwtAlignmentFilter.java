package com.example.WorkHub.auth;

import com.example.WorkHub.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Ensures {@code X-Tenant-ID} matches the {@code tenantId} claim in the JWT.
 * Without this, clients could send one tenant in the header and a token issued for another tenant.
 */
@Component
@RequiredArgsConstructor
public class TenantJwtAlignmentFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private static boolean isSkippedPath(String uri) {
        return uri.startsWith("/tenant")
                || uri.startsWith("/actuator")
                || uri.startsWith("/user")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isSkippedPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        String tenantHeader = request.getHeader("X-Tenant-ID");

        if (authHeader != null && authHeader.startsWith("Bearer ") && tenantHeader != null && !tenantHeader.isBlank()) {
            String token = authHeader.substring(7);
            try {
                long headerTenant = Long.parseLong(tenantHeader.trim());
                Long jwtTenant = jwtService.extractTenantId(token);
                if (jwtTenant == null || jwtTenant != headerTenant) {
                    response.sendError(HttpStatus.FORBIDDEN.value(), "X-Tenant-ID does not match the tenant in your token");
                    return;
                }
            } catch (NumberFormatException ex) {
                response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid X-Tenant-ID");
                return;
            } catch (Exception ex) {
                // Expired or malformed token — let JwtAuthFilter / security return 401 where applicable
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or expired token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
