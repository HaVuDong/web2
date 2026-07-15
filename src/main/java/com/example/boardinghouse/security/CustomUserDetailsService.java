package com.example.boardinghouse.security;

import com.example.boardinghouse.domain.entity.Tenant;
import com.example.boardinghouse.domain.entity.User;
import com.example.boardinghouse.repository.TenantRepository;
import com.example.boardinghouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Dịch vụ cung cấp thông tin người dùng cho Spring Security.
 * Hỗ trợ tìm kiếm cả Chủ trọ (User) và Khách thuê (Tenant).
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    /**
     * Tải thông tin người dùng dựa trên tên đăng nhập (ở đây là email).
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Tìm trong bảng users (Chủ trọ)
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            return new CustomUserDetails(userOpt.get());
        }

        // 2. Tìm trong bảng tenants (Khách thuê)
        // Vì TenantRepository chưa có findByEmail và lọc deleted, ta có thể phải implement thêm ở repository.
        // Nhưng tạm thời ta gọi từ repository custom (sẽ cập nhật TenantRepository sau)
        Tenant tenant = tenantRepository.findByEmailAndDeletedNot(email, true)
                .orElseThrow(() -> new UsernameNotFoundException("User/Tenant not found with email: " + email));
        
        return new CustomUserDetails(tenant);
    }
}
