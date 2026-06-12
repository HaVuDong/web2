package com.example.boardinghouse.dto.room;

import com.example.boardinghouse.domain.enums.RoomStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class UpdateRoomRequest {

    @NotBlank(message = "Room number is required")
    private String roomNumber;

    @PositiveOrZero(message = "Floor must be greater than or equal to 0")
    private Integer floor;

    @PositiveOrZero(message = "Area must be greater than or equal to 0")
    private Double area;

    @PositiveOrZero(message = "Base rent must be greater than or equal to 0")
    private Long baseRent;

    @PositiveOrZero(message = "Deposit must be greater than or equal to 0")
    private Long deposit;

    @Positive(message = "Max tenants must be greater than or equal to 1")
    private Integer maxTenants;

    private RoomStatus status;

    private String note;
}
