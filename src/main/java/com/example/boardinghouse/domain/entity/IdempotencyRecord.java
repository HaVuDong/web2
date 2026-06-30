package com.example.boardinghouse.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "idempotency_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {
    @Id
    private String id;

    @Indexed
    private String ownerId;

    @Indexed(unique = true)
    private String idempotencyKey;

    private String method;
    private String requestPath;
    
    // Status can be PROCESSING or COMPLETED or FAILED
    private String status;

    private Integer responseStatusCode;
    
    private Map<String, Object> responseBody;

    @CreatedDate
    private LocalDateTime createdAt;
    
    private LocalDateTime completedAt;
}
