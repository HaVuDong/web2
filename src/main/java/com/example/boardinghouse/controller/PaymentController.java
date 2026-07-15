package com.example.boardinghouse.controller;

import com.example.boardinghouse.common.dto.ApiResponse;
import com.example.boardinghouse.domain.entity.Payment;
import com.example.boardinghouse.dto.payment.ManualPaymentRequest;
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

    /**
     * API: Tạo link thanh toán (Mã QR/URL chuyển hướng) cho một hóa đơn thông qua cổng PayOS.
     * Endpoint: POST /api/invoices/{invoiceId}/payment-link
     */
    @PostMapping("/invoices/{invoiceId}/payment-link")
    public ApiResponse<PaymentLinkResponse> createPaymentLink(
            @PathVariable String invoiceId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String returnUrl,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String cancelUrl
    ) {
        PaymentLinkResponse response = paymentService.createPaymentLink(invoiceId, returnUrl, cancelUrl);
        return ApiResponse.success("Payment link created successfully", response);
    }

    @PostMapping("/invoices/{invoiceId}/payments/cash")
    public ApiResponse<Payment> recordCashPayment(
            @PathVariable String invoiceId,
            @Valid @RequestBody(required = false) ManualPaymentRequest request
    ) {
        Payment payment = paymentService.recordCashPayment(invoiceId, request);
        return ApiResponse.success("Cash payment recorded successfully", payment);
    }

    @PostMapping("/invoices/{invoiceId}/payments/bank-transfer")
    public ApiResponse<Payment> recordBankTransferPayment(
            @PathVariable String invoiceId,
            @Valid @RequestBody(required = false) ManualPaymentRequest request
    ) {
        Payment payment = paymentService.recordBankTransferPayment(invoiceId, request);
        return ApiResponse.success("Bank transfer payment recorded successfully", payment);
    }

    /**
     * API: Webhook nhận thông báo từ PayOS khi khách hàng thanh toán thành công (hoặc thất bại).
     * Hệ thống PayOS sẽ tự động gọi vào API này.
     * Endpoint: POST /api/webhooks/payos
     */
    @PostMapping("/webhooks/payos")
    public ApiResponse<Payment> handlePayosWebhook(@Valid @RequestBody PayosWebhookRequest request) {
        Payment payment = paymentService.handlePayosWebhook(request);
        return ApiResponse.success("PayOS webhook processed successfully", payment);
    }

    /**
     * API: Lấy chi tiết thông tin của một giao dịch thanh toán.
     * Endpoint: GET /api/payments/{id}
     */
    @GetMapping("/payments/{id}")
    public ApiResponse<Payment> getPaymentById(@PathVariable String id) {
        return ApiResponse.success(paymentService.getPaymentById(id));
    }

    /**
     * API: Lấy lịch sử tất cả các giao dịch thanh toán của một hóa đơn.
     * Endpoint: GET /api/invoices/{invoiceId}/payments
     */
    @GetMapping("/invoices/{invoiceId}/payments")
    public ApiResponse<List<Payment>> getPaymentsByInvoiceId(@PathVariable String invoiceId) {
        return ApiResponse.success(paymentService.getPaymentsByInvoiceId(invoiceId));
    }
}
