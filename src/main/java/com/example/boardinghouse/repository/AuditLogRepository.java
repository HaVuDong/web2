package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository (DAO) để thao tác với collection AuditLog (Lịch sử kiểm toán) trong MongoDB.
 */
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    Page<AuditLog> findByOwnerId(String ownerId, Pageable pageable);

    Page<AuditLog> findByOwnerIdAndEntityType(String ownerId, String entityType, Pageable pageable);

    Page<AuditLog> findByOwnerIdAndEntityId(String ownerId, String entityId, Pageable pageable);

    Page<AuditLog> findByOwnerIdAndAction(String ownerId, String action, Pageable pageable);

    Page<AuditLog> findByOwnerIdAndEntityTypeAndAction(String ownerId, String entityType, String action, Pageable pageable);
}
