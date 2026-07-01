package com.example.boardinghouse.domain.entity;

import com.example.boardinghouse.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Thực thể đại diện cho người dùng hệ thống (chủ trọ/quản lý).
 */
public class User {
    @Id
    private String id;
    private String email;
    private String passwordHash;
    private String name;
    private UserRole role;
    private boolean isActive;

    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}