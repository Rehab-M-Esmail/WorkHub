package com.example.WorkHub.repository;

import com.example.WorkHub.models.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByTenantId(Long tenantId);
    Optional<Project> findByNameAndTenantId(String name, Long tenantId);
}
