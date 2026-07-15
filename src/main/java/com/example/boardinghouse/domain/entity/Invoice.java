package com.example.boardinghouse.domain.entity;

import com.example.boardinghouse.domain.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "invoices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Thực thể đại diện cho hóa đơn thu tiền hàng tháng của phòng.
 */
public class Invoice implements SoftDeletable {
    @Id
    private String id;

    @Indexed
    private String ownerId;

    @Indexed
    private String roomId;

    @Indexed
    private String contractId;

    private Integer month;

    private Integer year;

    private Long rentAmount;

    private Long electricityOld;

    private Long electricityNew;

    private Long electricityUsage;

    private Long electricityPrice;

    private Long electricityAmount;

    private Long waterOld;

    private Long waterNew;

    private Long waterUsage;

    private Long waterPrice;

    private Long waterAmount;

    private Long wifiFee;

    private Long garbageFee;

    private Long parkingFee;

    private Long otherFees;

    private Long discountAmount;

    private Long totalAmount;

    @Indexed
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.UNPAID;

    private LocalDate dueDate;

    private LocalDateTime paidAt;

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
