package com.example.boardinghouse.controller;

import com.example.boardinghouse.common.dto.ApiResponse;
import com.example.boardinghouse.dto.dashboard.DashboardDebtsResponse;
import com.example.boardinghouse.dto.dashboard.DashboardRevenueResponse;
import com.example.boardinghouse.dto.dashboard.DashboardRoomsStatusResponse;
import com.example.boardinghouse.dto.dashboard.DashboardSummaryResponse;
import com.example.boardinghouse.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> getSummary() {
        return ApiResponse.success(dashboardService.getSummary());
    }

    @GetMapping("/revenue")
    public ApiResponse<DashboardRevenueResponse> getRevenue(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        return ApiResponse.success(dashboardService.getRevenue(month, year));
    }

    @GetMapping("/debts")
    public ApiResponse<DashboardDebtsResponse> getDebts() {
        return ApiResponse.success(dashboardService.getDebts());
    }

    @GetMapping("/rooms-status")
    public ApiResponse<DashboardRoomsStatusResponse> getRoomsStatus() {
        return ApiResponse.success(dashboardService.getRoomsStatus());
    }
}
