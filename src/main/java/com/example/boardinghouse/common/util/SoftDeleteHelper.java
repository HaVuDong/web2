package com.example.boardinghouse.common.util;

import com.example.boardinghouse.domain.entity.SoftDeletable;

import java.time.LocalDateTime;

/**
 * Utility class cung cấp helper methods cho soft delete.
 */
public final class SoftDeleteHelper {

    private SoftDeleteHelper() {
        // utility class
    }

    /**
     * Đánh dấu entity là đã xóa (soft delete).
     *
     * @param entity    Entity cần xóa mềm
     * @param deletedBy ID người thực hiện xóa
     */
    public static void markDeleted(SoftDeletable entity, String deletedBy) {
        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setDeletedBy(deletedBy);
    }

    /**
     * Đánh dấu entity là đã xóa (soft delete) kèm lý do.
     *
     * @param entity       Entity cần xóa mềm
     * @param deletedBy    ID người thực hiện xóa
     * @param deleteReason Lý do xóa
     */
    public static void markDeleted(SoftDeletable entity, String deletedBy, String deleteReason) {
        markDeleted(entity, deletedBy);
        entity.setDeleteReason(deleteReason);
    }
}
