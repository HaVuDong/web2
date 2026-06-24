package com.example.boardinghouse.dto.invoice;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDate;

@Data
/**
 * DTO cho yêu cầu cập nhật hóa đơn.
 */
public class UpdateInvoiceRequest {

    @PositiveOrZero(message = "Other fees must be greater than or equal to 0")
    private Long otherFees;

    @PositiveOrZero(message = "Discount amount must be greater than or equal to 0")
    private Long discountAmount;

    private LocalDate dueDate;

    private String note;
}
