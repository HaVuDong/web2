package com.example.boardinghouse.controller;

import com.example.boardinghouse.common.dto.ApiResponse;
import com.example.boardinghouse.domain.entity.AuditLog;
import com.example.boardinghouse.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditService auditService;

    /**
     * API: Lấy danh sách audit log theo owner, hỗ trợ filter và phân trang.
     * Endpoint: GET /api/audit-logs?entityType=&entityId=&action=&page=&size=
     */
    @GetMapping
    public ApiResponse<Page<AuditLog>> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<AuditLog> logs = auditService.query(entityType, entityId, action, page, size);
        return ApiResponse.success(logs);
    }
}
