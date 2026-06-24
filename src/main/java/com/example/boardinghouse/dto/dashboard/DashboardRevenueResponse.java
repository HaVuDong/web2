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
 * DTO trả về thống kê doanh thu theo thời gian.
 */
public class DashboardRevenueResponse {
    private Integer month;
    private Integer year;
    private Long expectedRevenue;
    private Long paidRevenue;
    private Long unpaidRevenue;
    private long invoiceCount;
    private long paidInvoices;
    private long unpaidInvoices;
}
