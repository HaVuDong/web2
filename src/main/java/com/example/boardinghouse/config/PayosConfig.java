package com.example.boardinghouse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PayosConfig {

    @Bean
    public RestClient payosRestClient(PayosProperties payosProperties) {
        return RestClient.builder()
                .baseUrl(payosProperties.getBaseUrl())
                .build();
    }
}
