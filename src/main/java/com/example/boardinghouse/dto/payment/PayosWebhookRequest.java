package com.example.boardinghouse.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * DTO nhận dữ liệu webhook từ PayOS trả về khi có giao dịch thanh toán.
 */
public class PayosWebhookRequest {
    @NotBlank(message = "Webhook code is required")
    private String code;

    @NotBlank(message = "Webhook desc is required")
    private String desc;

    @NotNull(message = "Webhook success flag is required")
    private Boolean success;

    private Map<String, Object> data;

    @NotBlank(message = "Webhook signature is required")
    private String signature;
}
