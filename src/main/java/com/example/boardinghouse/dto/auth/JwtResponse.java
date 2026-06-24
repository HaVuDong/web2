package com.example.boardinghouse.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * DTO chứa thông tin trả về sau khi đăng nhập thành công.
 */
public class JwtResponse {
    private String token;
    @Builder.Default
    private String type = "Bearer";
    private String email;
    private String name;
    private String role;
}
