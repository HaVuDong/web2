package com.example.boardinghouse.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayosCreatePaymentLinkResponse {
    private String checkoutUrl;
    private String qrCode;
}
