package com.example.boardinghouse.dto.room;

import com.example.boardinghouse.domain.enums.RoomStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRoomStatusRequest {

    @NotNull(message = "Room status is required")
    private RoomStatus status;
}
