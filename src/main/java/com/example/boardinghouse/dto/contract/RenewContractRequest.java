package com.example.boardinghouse.dto.contract;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RenewContractRequest {

    @NotNull(message = "New end date is required")
    private LocalDate newEndDate;

    @PositiveOrZero(message = "Monthly rent must be greater than or equal to 0")
    private Long monthlyRent;

    @PositiveOrZero(message = "Deposit must be greater than or equal to 0")
    private Long deposit;

    @Min(value = 1, message = "Payment due day must be from 1 to 31")
    @Max(value = 31, message = "Payment due day must be from 1 to 31")
    private Integer paymentDueDay;

    private String note;
}
