package com.example.boardinghouse.controller;

import com.example.boardinghouse.repository.IdempotencyRecordRepository;
import com.example.boardinghouse.domain.entity.MaintenanceRequest;
import com.example.boardinghouse.domain.enums.MaintenancePriority;
import com.example.boardinghouse.domain.enums.MaintenanceStatus;
import com.example.boardinghouse.security.CustomUserDetailsService;
import com.example.boardinghouse.security.JwtAuthFilter;
import com.example.boardinghouse.service.MaintenanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MaintenanceController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class MaintenanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaintenanceService maintenanceService;

    @MockBean
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void createMaintenanceRequestReturnsCreatedRequest() throws Exception {
        when(maintenanceService.createMaintenanceRequest(any())).thenReturn(maintenanceRequest(MaintenanceStatus.PENDING));

        mockMvc.perform(post("/api/maintenance-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": "room-1",
                                  "tenantId": "tenant-1",
                                  "title": "Fix water leak",
                                  "description": "Sink is leaking",
                                  "priority": "HIGH"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Maintenance request created successfully"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"));
    }

    @Test
    void createMaintenanceRequestRejectsMissingTitle() throws Exception {
        mockMvc.perform(post("/api/maintenance-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": "room-1",
                                  "priority": "HIGH"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getMaintenanceRequestsReturnsFilteredRequests() throws Exception {
        when(maintenanceService.getMaintenanceRequests(MaintenanceStatus.PENDING))
                .thenReturn(List.of(maintenanceRequest(MaintenanceStatus.PENDING)));

        mockMvc.perform(get("/api/maintenance-requests")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    void updateMaintenanceStatusReturnsUpdatedRequest() throws Exception {
        when(maintenanceService.updateMaintenanceStatus("maintenance-1", MaintenanceStatus.DONE))
                .thenReturn(maintenanceRequest(MaintenanceStatus.DONE));

        mockMvc.perform(patch("/api/maintenance-requests/maintenance-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DONE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Maintenance status updated successfully"))
                .andExpect(jsonPath("$.data.status").value("DONE"));
    }

    private MaintenanceRequest maintenanceRequest(MaintenanceStatus status) {
        return MaintenanceRequest.builder()
                .id("maintenance-1")
                .roomId("room-1")
                .tenantId("tenant-1")
                .title("Fix water leak")
                .description("Sink is leaking")
                .priority(MaintenancePriority.HIGH)
                .status(status)
                .build();
    }
}

