package com.example.boardinghouse.controller;

import com.example.boardinghouse.common.dto.ApiResponse;
import com.example.boardinghouse.dto.auth.JwtResponse;
import com.example.boardinghouse.dto.auth.LoginRequest;
import com.example.boardinghouse.security.CustomUserDetails;
import com.example.boardinghouse.security.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    /**
     * Xác thực người dùng khi đăng nhập.
     * Sử dụng AuthenticationManager để kiểm tra email và mật khẩu.
     * Nếu thành công, lưu thông tin xác thực vào SecurityContext và tạo JWT token.
     * Trả về token kèm theo thông tin cơ bản của người dùng.
     *
     * @param loginRequest Dữ liệu đăng nhập (email, password)
     * @return Phản hồi chứa JWT Token và thông tin user
     */
    @PostMapping("/login")
    public ApiResponse<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateToken((CustomUserDetails) authentication.getPrincipal());
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        JwtResponse response = JwtResponse.builder()
                .token(jwt)
                .email(userDetails.getUsername())
                .name(userDetails.getUser().getName())
                .role(userDetails.getUser().getRole().name())
                .build();

        return ApiResponse.success("Login successful", response);
    }
}
