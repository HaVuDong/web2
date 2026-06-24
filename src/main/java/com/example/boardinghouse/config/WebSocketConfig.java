package com.example.boardinghouse.config;

import com.example.boardinghouse.realtime.RealtimeWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

/**
 * Cấu hình WebSocket cho ứng dụng để hỗ trợ tính năng realtime (thời gian thực).
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final RealtimeWebSocketHandler realtimeWebSocketHandler;

    @Value("${app.cors.allowed-origin-patterns}")
    private String allowedOriginPatterns;

    /**
     * Đăng ký endpoint cho WebSocket.
     * Endpoint "/ws/realtime" sẽ được sử dụng bởi client (Frontend) để kết nối.
     * Cấu hình CORS để cho phép các domain được chỉ định trong file properties có thể kết nối.
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] originPatterns = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(pattern -> !pattern.isEmpty())
                .toArray(String[]::new);

        registry.addHandler(realtimeWebSocketHandler, "/ws/realtime")
                .setAllowedOriginPatterns(originPatterns);
    }
}
