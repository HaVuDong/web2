package com.example.boardinghouse.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Lớp cấu hình dùng để đọc các thông số liên quan đến cổng thanh toán PayOS từ file application.yml (hoặc properties).
 */
@Data
@Component
@ConfigurationProperties(prefix = "payos")
public class PayosProperties {
    private String clientId;
    private String apiKey;
    private String checksumKey;
    private String returnUrl;
    private String cancelUrl;
    private String webhookUrl;
    private String baseUrl;
}
