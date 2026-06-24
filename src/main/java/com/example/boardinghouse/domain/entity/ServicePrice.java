package com.example.boardinghouse.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "service_prices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Thực thể lưu trữ bảng giá dịch vụ (điện, nước, rác...) cho từng tòa nhà.
 */
public class ServicePrice {
    @Id
    private String id;

    @Indexed(unique = true)
    private String propertyId;

    private Long electricityPrice;

    private Long waterPrice;

    private Long wifiFee;

    private Long garbageFee;

    private Long parkingFee;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
