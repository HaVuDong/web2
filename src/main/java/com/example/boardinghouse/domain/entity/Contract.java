package com.example.boardinghouse.domain.entity;

import com.example.boardinghouse.domain.enums.ContractStatus;
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
import java.util.List;

@Document(collection = "contracts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Thực thể đại diện cho hợp đồng thuê phòng.
 */
public class Contract implements SoftDeletable {
    @Id
    private String id;

    @Indexed
    private String ownerId;

    @Indexed
    private String roomId;

    private List<String> tenantIds;

    private LocalDate startDate;

    private LocalDate endDate;

    private Long monthlyRent;

    private Long deposit;

    private Integer paymentDueDay;

    @Indexed
    @Builder.Default
    private ContractStatus status = ContractStatus.ACTIVE;

    private LocalDateTime terminatedAt;

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
