package com.example.boardinghouse.dto.invoice;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class GenerateInvoiceRequest {

    @NotBlank(message = "Room id is required")
    private String roomId;

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be from 1 to 12")
    @Max(value = 12, message = "Month must be from 1 to 12")
    private Integer month;

    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Year must be valid")
    @Max(value = 2100, message = "Year must be valid")
    private Integer year;

    @PositiveOrZero(message = "Other fees must be greater than or equal to 0")
    private Long otherFees;

    @PositiveOrZero(message = "Discount amount must be greater than or equal to 0")
    private Long discountAmount;

    private String note;
}
