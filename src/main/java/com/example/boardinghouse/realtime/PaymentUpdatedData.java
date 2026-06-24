package com.example.boardinghouse.realtime;

import com.example.boardinghouse.domain.enums.InvoiceStatus;
import com.example.boardinghouse.domain.enums.PaymentStatus;

import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) dùng để chứa dữ liệu chi tiết về một giao dịch thanh toán vừa được cập nhật.
 * Dữ liệu này sẽ được gửi qua WebSocket cho client.
 */
public record PaymentUpdatedData(
        String paymentId,
        String invoiceId,
        PaymentStatus paymentStatus,
        InvoiceStatus invoiceStatus,
        LocalDateTime paidAt
) {
}
