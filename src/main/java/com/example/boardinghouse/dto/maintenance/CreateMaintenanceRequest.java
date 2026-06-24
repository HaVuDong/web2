package com.example.boardinghouse.dto.maintenance;

import com.example.boardinghouse.domain.enums.MaintenancePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
/**
 * DTO cho yêu cầu tạo mới một yêu cầu bảo trì/sửa chữa.
 */
public class CreateMaintenanceRequest {
    @NotBlank(message = "Room id is required")
    private String roomId;

    private String tenantId;

    @NotBlank(message = "Maintenance title is required")
    private String title;

    private String description;

    @NotNull(message = "Maintenance priority is required")
    private MaintenancePriority priority;
}
