package com.example.boardinghouse.dto.payment;

import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ManualPaymentRequest {
    @Positive(message = "Payment amount must be greater than 0")
    private Long amount;

    private String note;
}
