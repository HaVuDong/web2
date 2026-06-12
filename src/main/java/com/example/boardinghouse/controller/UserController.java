package com.example.boardinghouse.controller;

import com.example.boardinghouse.common.dto.ApiResponse;
import com.example.boardinghouse.domain.entity.User;
import com.example.boardinghouse.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public ApiResponse<User> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        // Return current user but avoid returning passwordHash
        User user = userDetails.getUser();
        user.setPasswordHash(null);
        return ApiResponse.success(user);
    }
}
