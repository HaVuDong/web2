package com.example.boardinghouse.controller;

import com.example.boardinghouse.common.dto.ApiResponse;
import com.example.boardinghouse.domain.entity.Invoice;
import com.example.boardinghouse.dto.invoice.GenerateInvoiceRequest;
import com.example.boardinghouse.dto.invoice.GenerateMonthlyInvoiceRequest;
import com.example.boardinghouse.dto.invoice.MonthlyInvoiceGenerationResponse;
import com.example.boardinghouse.dto.invoice.UpdateInvoiceRequest;
import com.example.boardinghouse.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/invoices")
    public ApiResponse<List<Invoice>> getAllInvoices() {
        return ApiResponse.success(invoiceService.getAllInvoices());
    }

    @PostMapping("/invoices/generate")
    public ApiResponse<Invoice> generateInvoice(@Valid @RequestBody GenerateInvoiceRequest request) {
        Invoice invoice = invoiceService.generateInvoice(request);
        return ApiResponse.success("Invoice generated successfully", invoice);
    }

    @PostMapping("/invoices/generate-monthly")
    public ApiResponse<MonthlyInvoiceGenerationResponse> generateMonthlyInvoices(
            @Valid @RequestBody GenerateMonthlyInvoiceRequest request
    ) {
        MonthlyInvoiceGenerationResponse response = invoiceService.generateMonthlyInvoices(request);
        return ApiResponse.success("Monthly invoices generated successfully", response);
    }

    @GetMapping("/invoices/{id}")
    public ApiResponse<Invoice> getInvoiceById(@PathVariable String id) {
        return ApiResponse.success(invoiceService.getInvoiceById(id));
    }

    @PutMapping("/invoices/{id}")
    public ApiResponse<Invoice> updateInvoice(
            @PathVariable String id,
            @Valid @RequestBody UpdateInvoiceRequest request
    ) {
        Invoice invoice = invoiceService.updateInvoice(id, request);
        return ApiResponse.success("Invoice updated successfully", invoice);
    }

    @DeleteMapping("/invoices/{id}")
    public ApiResponse<Void> deleteInvoice(@PathVariable String id) {
        invoiceService.deleteInvoice(id);
        return ApiResponse.success("Invoice deleted successfully", null);
    }

    @PatchMapping("/invoices/{id}/mark-paid")
    public ApiResponse<Invoice> markPaid(@PathVariable String id) {
        Invoice invoice = invoiceService.markPaid(id);
        return ApiResponse.success("Invoice marked as paid successfully", invoice);
    }

    @PatchMapping("/invoices/{id}/cancel")
    public ApiResponse<Invoice> cancelInvoice(@PathVariable String id) {
        Invoice invoice = invoiceService.cancelInvoice(id);
        return ApiResponse.success("Invoice cancelled successfully", invoice);
    }

    @GetMapping("/rooms/{roomId}/invoices")
    public ApiResponse<List<Invoice>> getInvoicesByRoomId(@PathVariable String roomId) {
        return ApiResponse.success(invoiceService.getInvoicesByRoomId(roomId));
    }
}
