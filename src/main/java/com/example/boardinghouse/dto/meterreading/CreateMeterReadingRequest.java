package com.example.boardinghouse.dto.meterreading;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class CreateMeterReadingRequest {

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

    @NotNull(message = "Old electricity reading is required")
    @PositiveOrZero(message = "Old electricity reading must be greater than or equal to 0")
    private Long electricityOld;

    @NotNull(message = "New electricity reading is required")
    @PositiveOrZero(message = "New electricity reading must be greater than or equal to 0")
    private Long electricityNew;

    @NotNull(message = "Old water reading is required")
    @PositiveOrZero(message = "Old water reading must be greater than or equal to 0")
    private Long waterOld;

    @NotNull(message = "New water reading is required")
    @PositiveOrZero(message = "New water reading must be greater than or equal to 0")
    private Long waterNew;

    private String note;
}
