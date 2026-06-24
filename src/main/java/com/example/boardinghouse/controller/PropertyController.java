package com.example.boardinghouse.controller;

import com.example.boardinghouse.common.dto.ApiResponse;
import com.example.boardinghouse.domain.entity.Property;
import com.example.boardinghouse.dto.property.CreatePropertyRequest;
import com.example.boardinghouse.dto.property.UpdatePropertyRequest;
import com.example.boardinghouse.security.CustomUserDetails;
import com.example.boardinghouse.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    /**
     * API: Lấy danh sách toàn bộ tòa nhà/khu trọ.
     * Endpoint: GET /api/properties
     */
    @GetMapping
    public ApiResponse<List<Property>> getAllProperties() {
        return ApiResponse.success(propertyService.getAllProperties());
    }

    /**
     * API: Thêm mới một tòa nhà/khu trọ.
     * Endpoint: POST /api/properties
     */
    @PostMapping
    public ApiResponse<Property> createProperty(
            @Valid @RequestBody CreatePropertyRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String createdBy = resolveCreatedBy(userDetails);
        Property property = propertyService.createProperty(request, createdBy);
        return ApiResponse.success("Property created successfully", property);
    }

    /**
     * API: Lấy thông tin chi tiết một tòa nhà theo ID.
     * Endpoint: GET /api/properties/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<Property> getPropertyById(@PathVariable String id) {
        return ApiResponse.success(propertyService.getPropertyById(id));
    }

    /**
     * API: Cập nhật thông tin tòa nhà (tên, địa chỉ, mô tả...).
     * Endpoint: PUT /api/properties/{id}
     */
    @PutMapping("/{id}")
    public ApiResponse<Property> updateProperty(
            @PathVariable String id,
            @Valid @RequestBody UpdatePropertyRequest request
    ) {
        Property property = propertyService.updateProperty(id, request);
        return ApiResponse.success("Property updated successfully", property);
    }

    /**
     * API: Xóa tòa nhà khỏi hệ thống.
     * Endpoint: DELETE /api/properties/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProperty(@PathVariable String id) {
        propertyService.deleteProperty(id);
        return ApiResponse.success("Property deleted successfully", null);
    }

    /**
     * Trích xuất thông tin người dùng đang đăng nhập (ID hoặc email) để lưu vào trường createdBy.
     */
    private String resolveCreatedBy(CustomUserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }

        if (userDetails.getUser() == null) {
            return null;
        }

        if (userDetails.getUser().getId() != null) {
            return userDetails.getUser().getId();
        }

        return userDetails.getUser().getEmail();
    }
}
