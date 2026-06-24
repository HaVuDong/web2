package com.example.boardinghouse.dto.maintenance;

import com.example.boardinghouse.domain.enums.MaintenanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
/**
 * DTO cho yêu cầu cập nhật trạng thái bảo trì/sửa chữa.
 */
public class UpdateMaintenanceStatusRequest {
    @NotNull(message = "Maintenance status is required")
    private MaintenanceStatus status;
}
