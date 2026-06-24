package com.example.boardinghouse.controller;

import com.example.boardinghouse.domain.entity.Invoice;
import com.example.boardinghouse.domain.enums.InvoiceStatus;
import com.example.boardinghouse.security.CustomUserDetailsService;
import com.example.boardinghouse.security.JwtAuthFilter;
import com.example.boardinghouse.service.InvoiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InvoiceController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoiceService invoiceService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void generateInvoiceReturnsGeneratedInvoice() throws Exception {
        when(invoiceService.generateInvoice(any())).thenReturn(invoice(InvoiceStatus.UNPAID));

        mockMvc.perform(post("/api/invoices/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": "room-1",
                                  "month": 6,
                                  "year": 2026,
                                  "otherFees": 25000,
                                  "discountAmount": 10000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Invoice generated successfully"))
                .andExpect(jsonPath("$.data.totalAmount").value(3035000));
    }

    @Test
    void generateInvoiceRejectsInvalidMonth() throws Exception {
        mockMvc.perform(post("/api/invoices/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": "room-1",
                                  "month": 13,
                                  "year": 2026
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void markPaidReturnsPaidInvoice() throws Exception {
        when(invoiceService.markPaid("invoice-1")).thenReturn(invoice(InvoiceStatus.PAID));

        mockMvc.perform(patch("/api/invoices/invoice-1/mark-paid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Invoice marked as paid successfully"))
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    void cancelInvoiceReturnsCancelledInvoice() throws Exception {
        when(invoiceService.cancelInvoice("invoice-1")).thenReturn(invoice(InvoiceStatus.CANCELLED));

        mockMvc.perform(patch("/api/invoices/invoice-1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Invoice cancelled successfully"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    private Invoice invoice(InvoiceStatus status) {
        return Invoice.builder()
                .id("invoice-1")
                .roomId("room-1")
                .month(6)
                .year(2026)
                .totalAmount(3_035_000L)
                .status(status)
                .dueDate(LocalDate.of(2026, 6, 5))
                .build();
    }
}
