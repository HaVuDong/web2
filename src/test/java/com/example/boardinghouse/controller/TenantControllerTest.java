package com.example.boardinghouse.controller;

import com.example.boardinghouse.domain.entity.Tenant;
import com.example.boardinghouse.domain.enums.TenantStatus;
import com.example.boardinghouse.security.CustomUserDetailsService;
import com.example.boardinghouse.security.JwtAuthFilter;
import com.example.boardinghouse.service.TenantService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TenantController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TenantService tenantService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getTenantsReturnsFilteredTenants() throws Exception {
        Tenant tenant = tenant("tenant-1", TenantStatus.ACTIVE, "room-1");
        when(tenantService.getTenants("nguyen", TenantStatus.ACTIVE)).thenReturn(List.of(tenant));

        mockMvc.perform(get("/api/tenants")
                        .param("keyword", "nguyen")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("tenant-1"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }

    @Test
    void getTenantsRejectsInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/tenants")
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid request parameter: status"));
    }

    @Test
    void createTenantReturnsCreatedTenant() throws Exception {
        Tenant tenant = tenant("tenant-1", TenantStatus.ACTIVE, null);
        when(tenantService.createTenant(any())).thenReturn(tenant);

        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van A",
                                  "phone": "0909123456",
                                  "email": "tenant@gmail.com",
                                  "identityNumber": "123456789",
                                  "permanentAddress": "Ha Noi",
                                  "status": "ACTIVE",
                                  "note": "Khach moi"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tenant created successfully"))
                .andExpect(jsonPath("$.data.fullName").value("Nguyen Van A"));
    }

    @Test
    void createTenantRejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van A",
                                  "phone": "0909123456",
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getTenantsByRoomIdReturnsTenants() throws Exception {
        Tenant tenant = tenant("tenant-1", TenantStatus.ACTIVE, "room-1");
        when(tenantService.getTenantsByRoomId("room-1")).thenReturn(List.of(tenant));

        mockMvc.perform(get("/api/rooms/room-1/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].currentRoomId").value("room-1"));
    }

    @Test
    void deleteTenantMarksTenantLeft() throws Exception {
        doNothing().when(tenantService).markTenantLeft("tenant-1");

        mockMvc.perform(delete("/api/tenants/tenant-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tenant marked as left successfully"));
    }

    private Tenant tenant(String id, TenantStatus status, String currentRoomId) {
        return Tenant.builder()
                .id(id)
                .fullName("Nguyen Van A")
                .phone("0909123456")
                .email("tenant@gmail.com")
                .status(status)
                .currentRoomId(currentRoomId)
                .build();
    }
}
