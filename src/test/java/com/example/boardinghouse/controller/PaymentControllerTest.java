package com.example.boardinghouse.controller;

import com.example.boardinghouse.domain.entity.Payment;
import com.example.boardinghouse.domain.enums.PaymentProvider;
import com.example.boardinghouse.domain.enums.PaymentStatus;
import com.example.boardinghouse.dto.payment.PaymentLinkResponse;
import com.example.boardinghouse.security.CustomUserDetailsService;
import com.example.boardinghouse.security.JwtAuthFilter;
import com.example.boardinghouse.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void createPaymentLinkReturnsPaymentLink() throws Exception {
        when(paymentService.createPaymentLink("invoice-1")).thenReturn(PaymentLinkResponse.builder()
                .paymentId("payment-1")
                .invoiceId("invoice-1")
                .orderCode(123456789L)
                .amount(3_035_000L)
                .status(PaymentStatus.PENDING)
                .checkoutUrl("https://pay.payos.vn/checkout")
                .qrCode("qr-code")
                .build());

        mockMvc.perform(post("/api/invoices/invoice-1/payment-link"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment link created successfully"))
                .andExpect(jsonPath("$.data.orderCode").value(123456789L))
                .andExpect(jsonPath("$.data.checkoutUrl").value("https://pay.payos.vn/checkout"));
    }

    @Test
    void handlePayosWebhookReturnsProcessedPayment() throws Exception {
        when(paymentService.handlePayosWebhook(org.mockito.ArgumentMatchers.any())).thenReturn(payment(PaymentStatus.PAID));

        mockMvc.perform(post("/api/webhooks/payos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "00",
                                  "desc": "success",
                                  "success": true,
                                  "data": {
                                    "orderCode": 123456789,
                                    "amount": 3035000,
                                    "code": "00",
                                    "desc": "success"
                                  },
                                  "signature": "signature"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("PayOS webhook processed successfully"))
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    void getPaymentsByInvoiceReturnsPayments() throws Exception {
        when(paymentService.getPaymentsByInvoiceId("invoice-1")).thenReturn(List.of(payment(PaymentStatus.PENDING)));

        mockMvc.perform(get("/api/invoices/invoice-1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].invoiceId").value("invoice-1"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    private Payment payment(PaymentStatus status) {
        return Payment.builder()
                .id("payment-1")
                .invoiceId("invoice-1")
                .provider(PaymentProvider.PAYOS)
                .orderCode(123456789L)
                .amount(3_035_000L)
                .status(status)
                .checkoutUrl("https://pay.payos.vn/checkout")
                .qrCode("qr-code")
                .build();
    }
}
