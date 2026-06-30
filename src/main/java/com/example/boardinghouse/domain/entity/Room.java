package com.example.boardinghouse.domain.entity;

import com.example.boardinghouse.domain.enums.RoomStatus;
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

@Document(collection = "rooms")
@CompoundIndex(
        name = "unique_room_number_per_property",
        def = "{'propertyId': 1, 'roomNumber': 1}",
        unique = true
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Thực thể đại diện cho một phòng trọ.
 */
public class Room implements SoftDeletable {
    @Id
    private String id;

    @Indexed
    private String ownerId;

    @Indexed
    private String propertyId;

    private String roomNumber;

    private Integer floor;

    private Double area;

    private Long baseRent;

    private Long deposit;

    private Integer maxTenants;

    @Indexed
    @Builder.Default
    private RoomStatus status = RoomStatus.AVAILABLE;

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
