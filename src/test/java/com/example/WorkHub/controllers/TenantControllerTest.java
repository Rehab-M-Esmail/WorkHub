package com.example.WorkHub.controllers;

import com.example.WorkHub.dtos.TenantRequestDTO;
import com.example.WorkHub.dtos.TenantResponseDTO;
import com.example.WorkHub.services.TenantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @MockitoBean
    private TenantService tenantService;

    @Test
    @DisplayName("POST /tenant - Success")
    void createTenant_ShouldReturnCreatedTenant() throws Exception {
        // Arrange
        TenantRequestDTO request = new TenantRequestDTO("Zewail", "PREMIUM");
        TenantResponseDTO response = new TenantResponseDTO(1L, "Zewail", "PREMIUM");

        Mockito.when(tenantService.createTenant(Mockito.any(TenantRequestDTO.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Zewail"));
    }

    @Test
    @DisplayName("GET /tenant/{id} - Success")
    void getTenantById_ShouldReturnTenant() throws Exception {
        // Arrange
        TenantResponseDTO response = new TenantResponseDTO(1L, "Zewail", "FREE");
        Mockito.when(tenantService.getTenantById(1L)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/tenant/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Zewail"));
    }

    @Test
    @DisplayName("GET /tenant - List All")
    void getAllTenants_ShouldReturnList() throws Exception {
        // Arrange
        List<TenantResponseDTO> list = List.of(
                new TenantResponseDTO(1L, "Tenant A", "FREE"),
                new TenantResponseDTO(2L, "Tenant B", "PREMIUM")
        );
        Mockito.when(tenantService.getAllTenants()).thenReturn(list);

        // Act & Assert
        mockMvc.perform(get("/tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Tenant A"));
    }

    @Test
    @DisplayName("PATCH /tenant/{id} - Success")
    void updateTenant_ShouldReturnUpdatedTenant() throws Exception {
        // Arrange
        TenantRequestDTO updateRequest = new TenantRequestDTO("Updated Name", "ENTERPRISE");
        TenantResponseDTO updatedResponse = new TenantResponseDTO(1L, "Updated Name", "ENTERPRISE");

        Mockito.when(tenantService.updateTenant(Mockito.eq(1L), Mockito.any(TenantRequestDTO.class)))
                .thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(patch("/tenant/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("ENTERPRISE"));
    }

    @Test
    @DisplayName("DELETE /tenant/{id} - Success")
    void deleteTenant_ShouldReturnNoContent() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/tenant/1"))
                .andExpect(status().isOk());

        Mockito.verify(tenantService, Mockito.times(1)).deleteTenant(1L);
    }
}