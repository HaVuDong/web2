package com.example.boardinghouse.controller;

import com.example.boardinghouse.common.dto.ApiResponse;
import com.example.boardinghouse.domain.entity.MeterReading;
import com.example.boardinghouse.dto.meterreading.CreateMeterReadingRequest;
import com.example.boardinghouse.dto.meterreading.UpdateMeterReadingRequest;
import com.example.boardinghouse.service.MeterReadingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api")
@RequiredArgsConstructor
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    @GetMapping("/meter-readings")
    public ApiResponse<List<MeterReading>> getAllMeterReadings() {
        return ApiResponse.success(meterReadingService.getAllMeterReadings());
    }

    @PostMapping("/meter-readings")
    public ApiResponse<MeterReading> createMeterReading(@Valid @RequestBody CreateMeterReadingRequest request) {
        MeterReading meterReading = meterReadingService.createMeterReading(request);
        return ApiResponse.success("Meter reading created successfully", meterReading);
    }

    @GetMapping("/rooms/{roomId}/meter-readings")
    public ApiResponse<List<MeterReading>> getMeterReadingsByRoomId(@PathVariable String roomId) {
        return ApiResponse.success(meterReadingService.getMeterReadingsByRoomId(roomId));
    }

    @GetMapping("/rooms/{roomId}/latest-meter-reading")
    public ApiResponse<MeterReading> getLatestMeterReading(@PathVariable String roomId) {
        return ApiResponse.success(meterReadingService.getLatestMeterReading(roomId));
    }

    @PutMapping("/meter-readings/{id}")
    public ApiResponse<MeterReading> updateMeterReading(
            @PathVariable String id,
            @Valid @RequestBody UpdateMeterReadingRequest request
    ) {
        MeterReading meterReading = meterReadingService.updateMeterReading(id, request);
        return ApiResponse.success("Meter reading updated successfully", meterReading);
    }

    @DeleteMapping("/meter-readings/{id}")
    public ApiResponse<Void> deleteMeterReading(@PathVariable String id) {
        meterReadingService.deleteMeterReading(id);
        return ApiResponse.success("Meter reading deleted successfully", null);
    }
}
