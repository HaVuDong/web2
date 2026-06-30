package com.example.boardinghouse.common.controller;

import com.example.boardinghouse.common.dto.ApiResponse;
import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@Profile({"dev", "test"})
public class TestController {

    @GetMapping("/success")
    public ApiResponse<String> testSuccess() {
        return ApiResponse.success("Successful response data");
    }

    @GetMapping("/not-found")
    public ApiResponse<String> testNotFound() {
        throw new ResourceNotFoundException("Resource not found test");
    }

    @GetMapping("/bad-request")
    public ApiResponse<String> testBadRequest() {
        throw new BadRequestException("Bad request test");
    }

    @GetMapping("/internal-error")
    public ApiResponse<String> testInternalError() {
        throw new RuntimeException("Internal server error test");
    }
}
