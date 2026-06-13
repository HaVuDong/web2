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
public class PayosWebhookRequest {
    @NotBlank(message = "Webhook code is required")
    private String code;

    @NotBlank(message = "Webhook desc is required")
    private String desc;

    @NotNull(message = "Webhook success flag is required")
    private Boolean success;

    @NotNull(message = "Webhook data is required")
    private Map<String, Object> data;

    @NotBlank(message = "Webhook signature is required")
    private String signature;
}
