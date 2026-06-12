package com.example.boardinghouse.dto.serviceprice;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class UpdateServicePriceRequest {

    @NotNull(message = "Electricity price is required")
    @PositiveOrZero(message = "Electricity price must be greater than or equal to 0")
    private Long electricityPrice;

    @NotNull(message = "Water price is required")
    @PositiveOrZero(message = "Water price must be greater than or equal to 0")
    private Long waterPrice;

    @NotNull(message = "WiFi fee is required")
    @PositiveOrZero(message = "WiFi fee must be greater than or equal to 0")
    private Long wifiFee;

    @NotNull(message = "Garbage fee is required")
    @PositiveOrZero(message = "Garbage fee must be greater than or equal to 0")
    private Long garbageFee;

    @NotNull(message = "Parking fee is required")
    @PositiveOrZero(message = "Parking fee must be greater than or equal to 0")
    private Long parkingFee;
}
