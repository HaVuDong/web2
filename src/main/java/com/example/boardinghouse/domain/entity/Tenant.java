package com.example.boardinghouse.domain.entity;

import com.example.boardinghouse.domain.enums.TenantStatus;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "tenants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Thực thể đại diện cho khách thuê phòng.
 */
public class Tenant implements SoftDeletable {
    @Id
    private String id;

    @Indexed
    private String ownerId;

    private String fullName;

    @Indexed
    private String phone;

    private String email;

    private String identityNumber;

    private LocalDate dateOfBirth;

    private String permanentAddress;

    @Indexed
    private String currentRoomId;

    @Indexed
    @Builder.Default
    private TenantStatus status = TenantStatus.ACTIVE;

    private String note;

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
