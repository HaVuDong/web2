package com.example.boardinghouse.dto.meterreading;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * DTO cho yêu cầu khách thuê tự ghi chỉ số điện nước mới.
 * Tenant chỉ cần nhập số mới, số cũ sẽ tự động lấy từ tháng trước.
 * roomId tự động lấy từ phòng mà tenant đang ở.
 */
@Data
public class TenantMeterReadingRequest {

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be from 1 to 12")
    @Max(value = 12, message = "Month must be from 1 to 12")
    private Integer month;

    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Year must be valid")
    @Max(value = 2100, message = "Year must be valid")
    private Integer year;

    @NotNull(message = "New electricity reading is required")
    @PositiveOrZero(message = "New electricity reading must be greater than or equal to 0")
    private Long electricityNew;

    @NotNull(message = "New water reading is required")
    @PositiveOrZero(message = "New water reading must be greater than or equal to 0")
    private Long waterNew;

    private String note;
}
