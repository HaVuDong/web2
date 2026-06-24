package com.example.boardinghouse.controller;

import com.example.boardinghouse.common.dto.ApiResponse;
import com.example.boardinghouse.domain.entity.ServicePrice;
import com.example.boardinghouse.dto.serviceprice.UpdateServicePriceRequest;
import com.example.boardinghouse.service.ServicePriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties/{propertyId}/service-prices")
@RequiredArgsConstructor
public class ServicePriceController {

    private final ServicePriceService servicePriceService;

    /**
     * API: Lấy bảng giá dịch vụ (điện, nước, rác, wifi...) của một tòa nhà.
     * Endpoint: GET /api/properties/{propertyId}/service-prices
     */
    @GetMapping
    public ApiResponse<ServicePrice> getServicePrice(@PathVariable String propertyId) {
        return ApiResponse.success(servicePriceService.getServicePrice(propertyId));
    }

    /**
     * API: Cập nhật bảng giá dịch vụ cho một tòa nhà.
     * Endpoint: PUT /api/properties/{propertyId}/service-prices
     */
    @PutMapping
    public ApiResponse<ServicePrice> updateServicePrice(
            @PathVariable String propertyId,
            @Valid @RequestBody UpdateServicePriceRequest request
    ) {
        ServicePrice servicePrice = servicePriceService.updateServicePrice(propertyId, request);
        return ApiResponse.success("Service price updated successfully", servicePrice);
    }
}
