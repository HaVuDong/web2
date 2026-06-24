package com.example.boardinghouse.controller;

import com.example.boardinghouse.common.dto.ApiResponse;
import com.example.boardinghouse.domain.entity.MaintenanceRequest;
import com.example.boardinghouse.domain.enums.MaintenanceStatus;
import com.example.boardinghouse.dto.maintenance.CreateMaintenanceRequest;
import com.example.boardinghouse.dto.maintenance.UpdateMaintenanceRequest;
import com.example.boardinghouse.dto.maintenance.UpdateMaintenanceStatusRequest;
import com.example.boardinghouse.service.MaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance-requests")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    /**
     * API: Lấy danh sách yêu cầu bảo trì/sửa chữa. Có thể lọc theo trạng thái (status).
     * Endpoint: GET /api/maintenance-requests
     */
    @GetMapping
    public ApiResponse<List<MaintenanceRequest>> getMaintenanceRequests(
            @RequestParam(required = false) MaintenanceStatus status
    ) {
        return ApiResponse.success(maintenanceService.getMaintenanceRequests(status));
    }

    /**
     * API: Gửi yêu cầu bảo trì/sửa chữa mới.
     * Endpoint: POST /api/maintenance-requests
     */
    @PostMapping
    public ApiResponse<MaintenanceRequest> createMaintenanceRequest(
            @Valid @RequestBody CreateMaintenanceRequest request
    ) {
        MaintenanceRequest maintenanceRequest = maintenanceService.createMaintenanceRequest(request);
        return ApiResponse.success("Maintenance request created successfully", maintenanceRequest);
    }

    /**
     * API: Lấy thông tin chi tiết của một yêu cầu bảo trì theo ID.
     * Endpoint: GET /api/maintenance-requests/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<MaintenanceRequest> getMaintenanceRequestById(@PathVariable String id) {
        return ApiResponse.success(maintenanceService.getMaintenanceRequestById(id));
    }

    /**
     * API: Cập nhật nội dung của yêu cầu bảo trì.
     * Endpoint: PUT /api/maintenance-requests/{id}
     */
    @PutMapping("/{id}")
    public ApiResponse<MaintenanceRequest> updateMaintenanceRequest(
            @PathVariable String id,
            @Valid @RequestBody UpdateMaintenanceRequest request
    ) {
        MaintenanceRequest maintenanceRequest = maintenanceService.updateMaintenanceRequest(id, request);
        return ApiResponse.success("Maintenance request updated successfully", maintenanceRequest);
    }

    /**
     * API: Cập nhật trạng thái của yêu cầu bảo trì (Ví dụ: Từ PENDING sang IN_PROGRESS hoặc DONE).
     * Endpoint: PATCH /api/maintenance-requests/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<MaintenanceRequest> updateMaintenanceStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateMaintenanceStatusRequest request
    ) {
        MaintenanceRequest maintenanceRequest = maintenanceService.updateMaintenanceStatus(id, request.getStatus());
        return ApiResponse.success("Maintenance status updated successfully", maintenanceRequest);
    }

    /**
     * API: Xóa yêu cầu bảo trì.
     * Endpoint: DELETE /api/maintenance-requests/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMaintenanceRequest(@PathVariable String id) {
        maintenanceService.deleteMaintenanceRequest(id);
        return ApiResponse.success("Maintenance request deleted successfully", null);
    }
}
