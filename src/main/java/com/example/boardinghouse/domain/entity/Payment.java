package com.example.boardinghouse.domain.entity;

import com.example.boardinghouse.domain.enums.PaymentProvider;
import com.example.boardinghouse.domain.enums.PaymentStatus;
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
import java.util.Map;

@Document(collection = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    private String id;

    @Indexed
    private String invoiceId;

    private PaymentProvider provider;

    @Indexed(unique = true)
    private Long orderCode;

    private Long amount;

    @Indexed
    private PaymentStatus status;

    private String checkoutUrl;

    private String qrCode;

    private String payosTransactionId;

    private LocalDateTime paidAt;

    private Map<String, Object> rawWebhookData;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
