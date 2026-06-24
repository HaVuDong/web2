package com.example.boardinghouse.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * DTO trả về thống kê trạng thái các phòng (trống, đang ở...).
 */
public class DashboardRoomsStatusResponse {
    private long totalRooms;
    private long availableRooms;
    private long occupiedRooms;
    private long reservedRooms;
    private long maintenanceRooms;
}
