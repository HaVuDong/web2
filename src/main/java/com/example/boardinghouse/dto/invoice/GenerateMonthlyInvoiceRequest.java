package com.example.boardinghouse.dto.invoice;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerateMonthlyInvoiceRequest {

    @NotBlank(message = "Property id is required")
    private String propertyId;

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be from 1 to 12")
    @Max(value = 12, message = "Month must be from 1 to 12")
    private Integer month;

    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Year must be valid")
    @Max(value = 2100, message = "Year must be valid")
    private Integer year;
}
