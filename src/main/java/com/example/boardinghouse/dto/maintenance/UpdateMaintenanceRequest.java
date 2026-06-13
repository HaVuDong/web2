package com.example.boardinghouse.dto.maintenance;

import com.example.boardinghouse.domain.enums.MaintenancePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMaintenanceRequest {
    private String tenantId;

    @NotBlank(message = "Maintenance title is required")
    private String title;

    private String description;

    @NotNull(message = "Maintenance priority is required")
    private MaintenancePriority priority;
}
