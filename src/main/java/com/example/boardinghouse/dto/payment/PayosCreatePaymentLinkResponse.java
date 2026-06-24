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
 * DTO chứa kết quả trả về từ PayOS sau khi tạo link thanh toán thành công.
 */
public class PayosCreatePaymentLinkResponse {
    private String checkoutUrl;
    private String qrCode;
}
