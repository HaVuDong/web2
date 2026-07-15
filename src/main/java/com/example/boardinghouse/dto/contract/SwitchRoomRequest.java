package com.example.boardinghouse.dto.contract;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/**
 * DTO cho yêu cầu đổi phòng của hợp đồng đang hoạt động.
 */
public class SwitchRoomRequest {

    @NotBlank(message = "New room ID is required")
    private String newRoomId;

    private String note;
}
