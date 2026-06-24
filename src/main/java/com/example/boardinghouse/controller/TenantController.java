package com.example.boardinghouse.controller;

import com.example.boardinghouse.common.dto.ApiResponse;
import com.example.boardinghouse.domain.entity.Tenant;
import com.example.boardinghouse.domain.enums.TenantStatus;
import com.example.boardinghouse.dto.tenant.CreateTenantRequest;
import com.example.boardinghouse.dto.tenant.UpdateTenantRequest;
import com.example.boardinghouse.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    /**
     * API: Lấy danh sách khách thuê. Hỗ trợ tìm kiếm theo từ khóa và lọc theo trạng thái.
     * Endpoint: GET /api/tenants
     */
    @GetMapping("/tenants")
    public ApiResponse<List<Tenant>> getTenants(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) TenantStatus status
    ) {
        return ApiResponse.success(tenantService.getTenants(keyword, status));
    }

    /**
     * API: Thêm mới một khách thuê vào hệ thống.
     * Endpoint: POST /api/tenants
     */
    @PostMapping("/tenants")
    public ApiResponse<Tenant> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        Tenant tenant = tenantService.createTenant(request);
        return ApiResponse.success("Tenant created successfully", tenant);
    }

    /**
     * API: Lấy thông tin chi tiết một khách thuê theo ID.
     * Endpoint: GET /api/tenants/{id}
     */
    @GetMapping("/tenants/{id}")
    public ApiResponse<Tenant> getTenantById(@PathVariable String id) {
        return ApiResponse.success(tenantService.getTenantById(id));
    }

    /**
     * API: Cập nhật thông tin khách thuê (tên, số điện thoại, CMND/CCCD...).
     * Endpoint: PUT /api/tenants/{id}
     */
    @PutMapping("/tenants/{id}")
    public ApiResponse<Tenant> updateTenant(
            @PathVariable String id,
            @Valid @RequestBody UpdateTenantRequest request
    ) {
        Tenant tenant = tenantService.updateTenant(id, request);
        return ApiResponse.success("Tenant updated successfully", tenant);
    }

    /**
     * API: Đánh dấu khách thuê đã rời đi (xóa mềm).
     * Endpoint: DELETE /api/tenants/{id}
     */
    @DeleteMapping("/tenants/{id}")
    public ApiResponse<Void> markTenantLeft(@PathVariable String id) {
        tenantService.markTenantLeft(id);
        return ApiResponse.success("Tenant marked as left successfully", null);
    }

    /**
     * API: Lấy danh sách các khách thuê đang ở trong một phòng cụ thể.
     * Endpoint: GET /api/rooms/{roomId}/tenants
     */
    @GetMapping("/rooms/{roomId}/tenants")
    public ApiResponse<List<Tenant>> getTenantsByRoomId(@PathVariable String roomId) {
        return ApiResponse.success(tenantService.getTenantsByRoomId(roomId));
    }
}
