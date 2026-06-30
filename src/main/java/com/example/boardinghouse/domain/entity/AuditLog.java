package com.example.boardinghouse.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "audit_logs")
@CompoundIndex(name = "idx_owner_created", def = "{'ownerId': 1, 'createdAt': -1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Thực thể lưu trữ log kiểm toán cho tất cả thao tác quan trọng trong hệ thống.
 * Immutable record — không cho sửa/xóa sau khi tạo.
 */
public class AuditLog {
    @Id
    private String id;

    @Indexed
    private String ownerId;

    private String actorId;

    private String actorEmail;

    @Indexed
    private String action;

    @Indexed
    private String entityType;

    @Indexed
    private String entityId;

    private Map<String, Object> before;

    private Map<String, Object> after;

    private List<String> changedFields;

    private String requestId;

    private String idempotencyKey;

    private String ipAddress;

    private String userAgent;

    @Builder.Default
    private boolean success = true;

    private String errorMessage;

    @CreatedDate
    private LocalDateTime createdAt;
}
