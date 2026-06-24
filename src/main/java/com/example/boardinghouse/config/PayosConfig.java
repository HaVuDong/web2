package com.example.boardinghouse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Cấu hình RestClient để gọi API sang hệ thống thanh toán PayOS.
 */
@Configuration
public class PayosConfig {

    /**
     * Khởi tạo RestClient với BaseURL của PayOS (được cấu hình trong file properties).
     * Bean này sẽ được tiêm (inject) vào PayosRestGateway để sử dụng.
     */
    @Bean
    public RestClient payosRestClient(PayosProperties payosProperties) {
        return RestClient.builder()
                .baseUrl(payosProperties.getBaseUrl())
                .build();
    }
}
