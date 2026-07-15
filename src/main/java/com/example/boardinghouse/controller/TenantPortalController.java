package com.example.boardinghouse.controller;

import com.example.boardinghouse.common.dto.ApiResponse;
import com.example.boardinghouse.domain.entity.Invoice;
import com.example.boardinghouse.domain.entity.MeterReading;
import com.example.boardinghouse.domain.entity.Tenant;
import com.example.boardinghouse.dto.meterreading.TenantMeterReadingRequest;
import com.example.boardinghouse.security.CurrentUserService;
import com.example.boardinghouse.service.InvoiceService;
import com.example.boardinghouse.service.MeterReadingService;
import com.example.boardinghouse.service.TenantService;
import com.example.boardinghouse.repository.RoomRepository;
import com.example.boardinghouse.repository.PropertyRepository;
import com.example.boardinghouse.domain.entity.Room;
import com.example.boardinghouse.domain.entity.Property;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * Controller dành riêng cho khách thuê (Tenant Portal).
 * Các API trong này yêu cầu quyền ROLE_TENANT (đã cấu hình trong SecurityConfig).
 */
@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
public class TenantPortalController {

    private final TenantService tenantService;
    private final InvoiceService invoiceService;
    private final MeterReadingService meterReadingService;
    private final CurrentUserService currentUserService;
    private final RoomRepository roomRepository;
    private final PropertyRepository propertyRepository;

    /**
     * API: Lấy thông tin cá nhân của khách thuê đang đăng nhập.
     */
    @GetMapping("/me")
    public ApiResponse<Tenant> getMyProfile() {
        String tenantId = currentUserService.getTenantId();
        Tenant tenant = tenantService.getTenantById(tenantId);
        return ApiResponse.success(tenant);
    }

    /**
     * API: Lấy danh sách hóa đơn của phòng mà khách thuê đang ở.
     */
    @GetMapping("/invoices")
    public ApiResponse<List<Invoice>> getMyInvoices() {
        String tenantId = currentUserService.getTenantId();
        Tenant tenant = tenantService.getTenantById(tenantId);
        
        if (tenant.getCurrentRoomId() == null) {
            return ApiResponse.success(Collections.emptyList());
        }
        
        List<Invoice> invoices = invoiceService.getInvoicesByRoomId(tenant.getCurrentRoomId());
        return ApiResponse.success(invoices);
    }

    /**
     * API: Đổi mật khẩu cho khách thuê.
     */
    @org.springframework.web.bind.annotation.PutMapping("/password")
    public ApiResponse<Void> changePassword(@jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.example.boardinghouse.dto.auth.ChangePasswordRequest request) {
        String tenantId = currentUserService.getTenantId();
        tenantService.changePassword(tenantId, request.getOldPassword(), request.getNewPassword());
        return ApiResponse.success("Đổi mật khẩu thành công", null);
    }

    /**
     * API: Lấy thông tin phòng và nhà trọ của khách thuê.
     */
    @GetMapping("/rental-info")
    public ApiResponse<java.util.Map<String, Object>> getMyRentalInfo() {
        String tenantId = currentUserService.getTenantId();
        Tenant tenant = tenantService.getTenantById(tenantId);
        
        if (tenant.getCurrentRoomId() == null) {
            return ApiResponse.success(Collections.emptyMap());
        }
        
        Room room = roomRepository.findById(tenant.getCurrentRoomId()).orElse(null);
        if (room == null) {
            return ApiResponse.success(Collections.emptyMap());
        }
        
        Property property = propertyRepository.findById(room.getPropertyId()).orElse(null);
        
        java.util.Map<String, Object> info = new java.util.HashMap<>();
        info.put("room", room);
        info.put("property", property);
        return ApiResponse.success(info);
    }

    /**
     * API: Lấy lịch sử chỉ số điện nước của phòng mà khách thuê đang ở.
     * Endpoint: GET /api/tenant/meter-readings
     */
    @GetMapping("/meter-readings")
    public ApiResponse<List<MeterReading>> getMyMeterReadings() {
        String tenantId = currentUserService.getTenantId();
        Tenant tenant = tenantService.getTenantById(tenantId);
        List<MeterReading> readings = meterReadingService.getTenantMeterReadings(tenant);
        return ApiResponse.success(readings);
    }

    /**
     * API: Khách thuê tự ghi chỉ số điện nước mới cho phòng mình đang ở.
     * Chỉ cần nhập số mới (electricityNew, waterNew), số cũ tự động lấy từ tháng trước.
     * Hạn chót nhập: trước ngày 5 hàng tháng.
     * Endpoint: POST /api/tenant/meter-readings
     */
    @PostMapping("/meter-readings")
    public ApiResponse<MeterReading> submitMeterReading(@Valid @RequestBody TenantMeterReadingRequest request) {
        String tenantId = currentUserService.getTenantId();
        Tenant tenant = tenantService.getTenantById(tenantId);
        MeterReading reading = meterReadingService.createTenantMeterReading(request, tenant);
        return ApiResponse.success("Ghi chỉ số thành công", reading);
    }
}

