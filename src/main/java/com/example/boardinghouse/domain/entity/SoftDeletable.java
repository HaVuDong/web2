package com.example.boardinghouse.domain.entity;

import java.time.LocalDateTime;

/**
 * Interface marker cho các entity hỗ trợ soft delete.
 * Các entity implement interface này sẽ có các field:
 * deleted, deletedAt, deletedBy, deleteReason.
 */
public interface SoftDeletable {
    Boolean getDeleted();
    void setDeleted(Boolean deleted);
    LocalDateTime getDeletedAt();
    void setDeletedAt(LocalDateTime deletedAt);
    String getDeletedBy();
    void setDeletedBy(String deletedBy);
    String getDeleteReason();
    void setDeleteReason(String deleteReason);
}
