package com.example.boardinghouse.domain.entity;

import com.example.boardinghouse.domain.enums.MaintenancePriority;
import com.example.boardinghouse.domain.enums.MaintenanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "maintenance_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Thực thể đại diện cho một yêu cầu bảo trì, sửa chữa từ khách thuê.
 */
public class MaintenanceRequest implements SoftDeletable {
    @Id
    private String id;

    @Indexed
    private String ownerId;

    @Indexed
    private String roomId;

    private String tenantId;

    private String title;

    private String description;

    private MaintenancePriority priority;

    @Indexed
    @Builder.Default
    private MaintenanceStatus status = MaintenanceStatus.PENDING;

    private LocalDateTime completedAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @Builder.Default
    private Boolean deleted = false;
    private LocalDateTime deletedAt;
    private String deletedBy;
    private String deleteReason;
}
