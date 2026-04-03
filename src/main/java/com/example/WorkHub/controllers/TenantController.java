package com.example.WorkHub.controllers;

import com.example.WorkHub.dtos.TenantRequestDTO;
import com.example.WorkHub.dtos.TenantResponseDTO;
import com.example.WorkHub.models.Tenant;
import com.example.WorkHub.services.TenantService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tenant")
public class TenantController {
    private TenantService tenantService;
    public TenantController(TenantService tenantService){
        this.tenantService = tenantService;
    }

    @PostMapping("")
    public TenantResponseDTO createTenant(@RequestBody TenantRequestDTO tenantRequestDTO){
        Tenant tenant = new Tenant();
        tenant.setName(tenantRequestDTO.name());
        tenant.setPlan(tenantRequestDTO.plan());
        return this.tenantService.createTenant(tenantRequestDTO);
    }

    @GetMapping("")
    public List<TenantResponseDTO> getAllTenants(){
        return this.tenantService.getAllTenants();
    }

    @GetMapping("/{id}")
    public TenantResponseDTO getTenantById(@PathVariable Long id){
        return this.tenantService.getTenantById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteTenantById(@PathVariable Long id){
        this.tenantService.deleteTenant(id);
    }

    @PatchMapping("/{id}")
    public TenantResponseDTO updateTenantById(@PathVariable Long id, @RequestBody TenantRequestDTO tenantRequestDTO){
        return this.tenantService.updateTenant(id, tenantRequestDTO);
    }

}
