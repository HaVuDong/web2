package com.example.boardinghouse.domain.entity;

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

import java.time.LocalDateTime;

@Document(collection = "meter_readings")
@CompoundIndex(
        name = "unique_meter_per_room_month",
        def = "{'roomId': 1, 'month': 1, 'year': 1}",
        unique = true
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Thực thể lưu trữ chỉ số đồng hồ điện nước hàng tháng của từng phòng.
 */
public class MeterReading implements SoftDeletable {
    @Id
    private String id;

    @Indexed
    private String ownerId;

    @Indexed
    private String roomId;

    private Integer month;

    private Integer year;

    private Long electricityOld;

    private Long electricityNew;

    private Long waterOld;

    private Long waterNew;

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
