package com.example.WorkHub.controllers;

import com.example.WorkHub.dtos.TenantRequestDTO;
import com.example.WorkHub.dtos.TenantResponseDTO;
import com.example.WorkHub.models.Tenant;
import com.example.WorkHub.services.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tenant")
@Tag(name = "Tenants", description = "Tenant CRUD operations")
public class TenantController {
    private TenantService tenantService;
    public TenantController(TenantService tenantService){
        this.tenantService = tenantService;
    }

    @Operation(summary = "Create a tenant", responses = {
            @ApiResponse(responseCode = "200", description = "Tenant created")
    })
    @PostMapping("")
    public TenantResponseDTO createTenant(@RequestBody TenantRequestDTO tenantRequestDTO){
        Tenant tenant = new Tenant();
        tenant.setName(tenantRequestDTO.name());
        tenant.setPlan(tenantRequestDTO.plan());
        return this.tenantService.createTenant(tenantRequestDTO);
    }

    @Operation(summary = "List all tenants")
    @GetMapping("")
    public List<TenantResponseDTO> getAllTenants(){
        return this.tenantService.getAllTenants();
    }

    @Operation(summary = "Get tenant by ID", responses = {
            @ApiResponse(responseCode = "200", description = "Tenant found"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    @GetMapping("/{id}")
    public TenantResponseDTO getTenantById(@PathVariable Long id){
        return this.tenantService.getTenantById(id);
    }

    @Operation(summary = "Delete tenant by ID")
    @DeleteMapping("/{id}")
    public void deleteTenantById(@PathVariable Long id){
        this.tenantService.deleteTenant(id);
    }

    @Operation(summary = "Update tenant by ID", responses = {
            @ApiResponse(responseCode = "200", description = "Tenant updated"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    @PatchMapping("/{id}")
    public TenantResponseDTO updateTenantById(@PathVariable Long id, @RequestBody TenantRequestDTO tenantRequestDTO){
        return this.tenantService.updateTenant(id, tenantRequestDTO);
    }
}
