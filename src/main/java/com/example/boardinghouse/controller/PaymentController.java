package com.example.boardinghouse.controller;

import com.example.boardinghouse.common.dto.ApiResponse;
import com.example.boardinghouse.domain.entity.Payment;
import com.example.boardinghouse.dto.payment.PaymentLinkResponse;
import com.example.boardinghouse.dto.payment.PayosWebhookRequest;
import com.example.boardinghouse.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/invoices/{invoiceId}/payment-link")
    public ApiResponse<PaymentLinkResponse> createPaymentLink(@PathVariable String invoiceId) {
        PaymentLinkResponse response = paymentService.createPaymentLink(invoiceId);
        return ApiResponse.success("Payment link created successfully", response);
    }

    @PostMapping("/webhooks/payos")
    public ApiResponse<Payment> handlePayosWebhook(@Valid @RequestBody PayosWebhookRequest request) {
        Payment payment = paymentService.handlePayosWebhook(request);
        return ApiResponse.success("PayOS webhook processed successfully", payment);
    }

    @GetMapping("/payments/{id}")
    public ApiResponse<Payment> getPaymentById(@PathVariable String id) {
        return ApiResponse.success(paymentService.getPaymentById(id));
    }

    @GetMapping("/invoices/{invoiceId}/payments")
    public ApiResponse<List<Payment>> getPaymentsByInvoiceId(@PathVariable String invoiceId) {
        return ApiResponse.success(paymentService.getPaymentsByInvoiceId(invoiceId));
    }
}
