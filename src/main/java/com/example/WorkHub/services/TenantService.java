package com.example.WorkHub.services;

import com.example.WorkHub.dtos.TenantRequestDTO;
import com.example.WorkHub.dtos.TenantResponseDTO;
import com.example.WorkHub.models.Tenant;
import com.example.WorkHub.repository.TenantRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TenantService {
    TenantRepository tenantRepository;
    public TenantService(TenantRepository tenantRepository){
        this.tenantRepository = tenantRepository;
    }

    public TenantResponseDTO createTenant(TenantRequestDTO tenantRequestDTO){
        Tenant tenant = new Tenant();
        tenant.setName(tenantRequestDTO.name());
        tenant.setPlan(tenantRequestDTO.plan());
        this.tenantRepository.save(tenant);
        return new TenantResponseDTO(tenant.getId(), tenant.getName(), tenant.getPlan());
    }

    public void deleteTenant(Long id){
        this.tenantRepository.deleteById(id);
    }

    public Tenant getTenantByName(String name) {
        return this.tenantRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + name));
    }

    public TenantResponseDTO getTenantById(Long id){
        Tenant tenant = this.tenantRepository.findById(id).orElseThrow(()-> new RuntimeException("Tenant not found"));
        return new TenantResponseDTO(tenant.getId(), tenant.getName(), tenant.getPlan());
    }

    public List<TenantResponseDTO> getAllTenants() {
        return tenantRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private TenantResponseDTO mapToDTO(Tenant tenant) {
        return new TenantResponseDTO(tenant.getId(), tenant.getName(), tenant.getPlan());
    }

    public TenantResponseDTO updateTenant(Long id, TenantRequestDTO request){
        Tenant existing = this.tenantRepository.findById(id).orElseThrow(()-> new RuntimeException("Tenant not found"));
        if (request.name() != null) existing.setName(request.name());
        if (request.plan() != null) existing.setPlan(request.plan());
        tenantRepository.save(existing);
        this.tenantRepository.save(existing);
        return new TenantResponseDTO(existing.getId(), existing.getName(), existing.getPlan());
    }
}
