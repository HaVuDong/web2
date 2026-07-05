package com.example.boardinghouse.config;

import com.example.boardinghouse.security.CustomUserDetailsService;
import com.example.boardinghouse.security.IdempotencyFilter;
import com.example.boardinghouse.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final IdempotencyFilter idempotencyFilter;

    @Value("${app.cors.allowed-origin-patterns}")
    private String allowedOriginPatterns;

    /**
     * Cấu hình nhà cung cấp xác thực (AuthenticationProvider).
     * Sử dụng DaoAuthenticationProvider để lấy thông tin user từ CSDL thông qua userDetailsService
     * và kiểm tra mật khẩu bằng passwordEncoder.
     *
     * @return DaoAuthenticationProvider đã được cấu hình
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Cung cấp AuthenticationManager để Spring Security quản lý quá trình xác thực.
     *
     * @param config Cấu hình mặc định của Spring
     * @return AuthenticationManager
     * @throws Exception nếu có lỗi khởi tạo
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Cấu hình bộ mã hóa mật khẩu sử dụng thuật toán BCrypt.
     * Giúp mã hóa mật khẩu trước khi lưu và kiểm tra mật khẩu khi đăng nhập.
     *
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Cấu hình chuỗi bộ lọc bảo mật (Security Filter Chain).
     * - Tắt CSRF (vì dùng JWT).
     * - Cấu hình CORS.
     * - Cấu hình quản lý Session thành STATELESS (không lưu session trên server).
     * - Phân quyền các endpoint (những URL nào được phép truy cập tự do, những URL nào cần đăng nhập).
     * - Thêm bộ lọc JWT (jwtAuthFilter) vào trước bộ lọc xác thực mặc định của Spring.
     *
     * @param http Đối tượng cấu hình HTTP security
     * @return SecurityFilterChain
     * @throws Exception nếu có lỗi cấu hình
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/webhooks/payos").permitAll()
                .requestMatchers("/ws/realtime").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(idempotencyFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }

    /**
     * Cấu hình Cross-Origin Resource Sharing (CORS).
     * Cho phép frontend (từ các domain khác) có thể gọi API tới backend.
     * Các origin hợp lệ được cấu hình trong file properties (app.cors.allowed-origin-patterns).
     *
     * @return Cấu hình CORS mặc định cho toàn bộ các endpoint (/**)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(
            Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(pattern -> !pattern.isEmpty())
                .toList()
        );
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Idempotency-Key"));
        configuration.setExposedHeaders(List.of("Authorization", "Idempotency-Key"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
