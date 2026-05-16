package com.example.WorkHub.config.multitenancy;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Reads the tenant id from {@code X-Tenant-ID} (canonical). Falls back to {@code Tenant-ID}
 * because clients often confuse the header name.
 */
public final class TenantHeaderResolver {

    private TenantHeaderResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String v = request.getHeader("X-Tenant-ID");
        if (v != null && !v.isBlank()) {
            return v.trim();
        }
        v = request.getHeader("Tenant-ID");
        if (v != null && !v.isBlank()) {
            return v.trim();
        }
        return null;
    }
}
