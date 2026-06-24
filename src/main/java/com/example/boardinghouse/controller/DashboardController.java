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

    /**
     * API: Lấy các số liệu tổng quan hiển thị trên màn hình chính (Dashboard).
     * Endpoint: GET /api/dashboard/summary
     */
    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> getSummary() {
        return ApiResponse.success(dashboardService.getSummary());
    }

    /**
     * API: Lấy báo cáo doanh thu theo tháng/năm.
     * Endpoint: GET /api/dashboard/revenue
     */
    @GetMapping("/revenue")
    public ApiResponse<DashboardRevenueResponse> getRevenue(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        return ApiResponse.success(dashboardService.getRevenue(month, year));
    }

    /**
     * API: Lấy thông tin các khoản nợ (hóa đơn chưa thanh toán).
     * Endpoint: GET /api/dashboard/debts
     */
    @GetMapping("/debts")
    public ApiResponse<DashboardDebtsResponse> getDebts() {
        return ApiResponse.success(dashboardService.getDebts());
    }

    /**
     * API: Lấy thống kê trạng thái của tất cả các phòng (trống, đã thuê, bảo trì...).
     * Endpoint: GET /api/dashboard/rooms-status
     */
    @GetMapping("/rooms-status")
    public ApiResponse<DashboardRoomsStatusResponse> getRoomsStatus() {
        return ApiResponse.success(dashboardService.getRoomsStatus());
    }
}
