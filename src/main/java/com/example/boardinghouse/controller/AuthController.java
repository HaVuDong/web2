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
