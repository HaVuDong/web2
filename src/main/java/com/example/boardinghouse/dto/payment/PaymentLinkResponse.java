package com.example.boardinghouse.dto.payment;

import com.example.boardinghouse.domain.entity.Payment;
import com.example.boardinghouse.domain.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLinkResponse {
    private String paymentId;
    private String invoiceId;
    private Long orderCode;
    private Long amount;
    private PaymentStatus status;
    private String checkoutUrl;
    private String qrCode;

    public static PaymentLinkResponse from(Payment payment) {
        return PaymentLinkResponse.builder()
                .paymentId(payment.getId())
                .invoiceId(payment.getInvoiceId())
                .orderCode(payment.getOrderCode())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .checkoutUrl(payment.getCheckoutUrl())
                .qrCode(payment.getQrCode())
                .build();
    }
}
