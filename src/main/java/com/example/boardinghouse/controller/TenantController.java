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

    @GetMapping("/tenants")
    public ApiResponse<List<Tenant>> getTenants(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) TenantStatus status
    ) {
        return ApiResponse.success(tenantService.getTenants(keyword, status));
    }

    @PostMapping("/tenants")
    public ApiResponse<Tenant> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        Tenant tenant = tenantService.createTenant(request);
        return ApiResponse.success("Tenant created successfully", tenant);
    }

    @GetMapping("/tenants/{id}")
    public ApiResponse<Tenant> getTenantById(@PathVariable String id) {
        return ApiResponse.success(tenantService.getTenantById(id));
    }

    @PutMapping("/tenants/{id}")
    public ApiResponse<Tenant> updateTenant(
            @PathVariable String id,
            @Valid @RequestBody UpdateTenantRequest request
    ) {
        Tenant tenant = tenantService.updateTenant(id, request);
        return ApiResponse.success("Tenant updated successfully", tenant);
    }

    @DeleteMapping("/tenants/{id}")
    public ApiResponse<Void> markTenantLeft(@PathVariable String id) {
        tenantService.markTenantLeft(id);
        return ApiResponse.success("Tenant marked as left successfully", null);
    }

    @GetMapping("/rooms/{roomId}/tenants")
    public ApiResponse<List<Tenant>> getTenantsByRoomId(@PathVariable String roomId) {
        return ApiResponse.success(tenantService.getTenantsByRoomId(roomId));
    }
}
