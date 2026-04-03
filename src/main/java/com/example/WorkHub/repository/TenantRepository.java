package com.example.WorkHub.repository;

import com.example.WorkHub.models.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    public Optional<Tenant> findByName(String name);
}
