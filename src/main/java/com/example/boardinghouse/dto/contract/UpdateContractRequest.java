package com.example.boardinghouse.dto.contract;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDate;

@Data
/**
 * DTO cho yêu cầu cập nhật thông tin hợp đồng.
 */
public class UpdateContractRequest {

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Monthly rent is required")
    @PositiveOrZero(message = "Monthly rent must be greater than or equal to 0")
    private Long monthlyRent;

    @NotNull(message = "Deposit is required")
    @PositiveOrZero(message = "Deposit must be greater than or equal to 0")
    private Long deposit;

    @NotNull(message = "Payment due day is required")
    @Min(value = 1, message = "Payment due day must be from 1 to 31")
    @Max(value = 31, message = "Payment due day must be from 1 to 31")
    private Integer paymentDueDay;

    private String note;
}
