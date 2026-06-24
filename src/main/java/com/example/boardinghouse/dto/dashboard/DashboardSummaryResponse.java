package com.example.boardinghouse.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * DTO trả về dữ liệu tổng quan cho trang chủ thống kê.
 */
public class DashboardSummaryResponse {
    private long totalRooms;
    private long occupiedRooms;
    private long availableRooms;
    private long maintenanceRooms;
    private Long monthlyExpectedRevenue;
    private Long monthlyPaidRevenue;
    private Long monthlyUnpaidRevenue;
    private long unpaidInvoices;
    private long pendingMaintenanceRequests;
}
