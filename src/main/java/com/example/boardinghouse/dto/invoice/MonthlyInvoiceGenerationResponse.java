package com.example.boardinghouse.dto.invoice;

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
public class MonthlyInvoiceGenerationResponse {
    private List<Invoice> createdInvoices;
    private List<String> skippedRooms;
    private List<String> errors;
}
