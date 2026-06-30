package com.example.boardinghouse.controller;

import com.example.boardinghouse.repository.IdempotencyRecordRepository;
import com.example.boardinghouse.dto.dashboard.DashboardDebtsResponse;
import com.example.boardinghouse.dto.dashboard.DashboardRevenueResponse;
import com.example.boardinghouse.dto.dashboard.DashboardRoomsStatusResponse;
import com.example.boardinghouse.dto.dashboard.DashboardSummaryResponse;
import com.example.boardinghouse.security.CustomUserDetailsService;
import com.example.boardinghouse.security.JwtAuthFilter;
import com.example.boardinghouse.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getSummaryReturnsDashboardSummary() throws Exception {
        when(dashboardService.getSummary()).thenReturn(DashboardSummaryResponse.builder()
                .totalRooms(20L)
                .occupiedRooms(15L)
                .availableRooms(4L)
                .maintenanceRooms(1L)
                .monthlyExpectedRevenue(30_000_000L)
                .monthlyPaidRevenue(20_000_000L)
                .monthlyUnpaidRevenue(10_000_000L)
                .unpaidInvoices(3L)
                .pendingMaintenanceRequests(2L)
                .build());

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRooms").value(20L))
                .andExpect(jsonPath("$.data.monthlyPaidRevenue").value(20_000_000L))
                .andExpect(jsonPath("$.data.pendingMaintenanceRequests").value(2L));
    }

    @Test
    void getRevenueReturnsMonthlyRevenue() throws Exception {
        when(dashboardService.getRevenue(6, 2026)).thenReturn(DashboardRevenueResponse.builder()
                .month(6)
                .year(2026)
                .expectedRevenue(600L)
                .paidRevenue(100L)
                .unpaidRevenue(500L)
                .invoiceCount(3L)
                .paidInvoices(1L)
                .unpaidInvoices(2L)
                .build());

        mockMvc.perform(get("/api/dashboard/revenue")
                        .param("month", "6")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.expectedRevenue").value(600L))
                .andExpect(jsonPath("$.data.unpaidInvoices").value(2L));
    }

    @Test
    void getDebtsReturnsDebtSummary() throws Exception {
        when(dashboardService.getDebts()).thenReturn(DashboardDebtsResponse.builder()
                .totalDebt(320L)
                .debtInvoiceCount(3L)
                .invoices(List.of())
                .build());

        mockMvc.perform(get("/api/dashboard/debts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalDebt").value(320L))
                .andExpect(jsonPath("$.data.debtInvoiceCount").value(3L));
    }

    @Test
    void getRoomsStatusReturnsCounts() throws Exception {
        when(dashboardService.getRoomsStatus()).thenReturn(DashboardRoomsStatusResponse.builder()
                .totalRooms(20L)
                .availableRooms(4L)
                .occupiedRooms(15L)
                .reservedRooms(0L)
                .maintenanceRooms(1L)
                .build());

        mockMvc.perform(get("/api/dashboard/rooms-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.occupiedRooms").value(15L))
                .andExpect(jsonPath("$.data.maintenanceRooms").value(1L));
    }
}

