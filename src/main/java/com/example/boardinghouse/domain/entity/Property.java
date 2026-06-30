package com.example.boardinghouse.domain.entity;

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

@Document(collection = "properties")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Thực thể đại diện cho một tòa nhà hoặc khu trọ.
 */
public class Property implements SoftDeletable {
    @Id
    private String id;

    @Indexed
    private String ownerId;

    private String name;

    private String address;

    private String description;

    private String createdBy;

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
