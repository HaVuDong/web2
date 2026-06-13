package com.example.boardinghouse.dto.dashboard;

import com.example.boardinghouse.domain.entity.Invoice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDebtsResponse {
    private Long totalDebt;
    private long debtInvoiceCount;
    private List<Invoice> invoices;
}
