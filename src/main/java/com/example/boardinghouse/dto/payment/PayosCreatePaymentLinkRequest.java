package com.example.boardinghouse.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * DTO cho yêu cầu tạo link thanh toán gửi lên PayOS.
 */
public class PayosCreatePaymentLinkRequest {
    private Long orderCode;
    private Long amount;
    private String description;
    private String returnUrl;
    private String cancelUrl;
}
