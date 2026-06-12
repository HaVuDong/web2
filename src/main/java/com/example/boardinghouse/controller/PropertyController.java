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

    @GetMapping
    public ApiResponse<List<Property>> getAllProperties() {
        return ApiResponse.success(propertyService.getAllProperties());
    }

    @PostMapping
    public ApiResponse<Property> createProperty(
            @Valid @RequestBody CreatePropertyRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String createdBy = resolveCreatedBy(userDetails);
        Property property = propertyService.createProperty(request, createdBy);
        return ApiResponse.success("Property created successfully", property);
    }

    @GetMapping("/{id}")
    public ApiResponse<Property> getPropertyById(@PathVariable String id) {
        return ApiResponse.success(propertyService.getPropertyById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Property> updateProperty(
            @PathVariable String id,
            @Valid @RequestBody UpdatePropertyRequest request
    ) {
        Property property = propertyService.updateProperty(id, request);
        return ApiResponse.success("Property updated successfully", property);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProperty(@PathVariable String id) {
        propertyService.deleteProperty(id);
        return ApiResponse.success("Property deleted successfully", null);
    }

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
