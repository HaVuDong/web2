package com.example.boardinghouse.controller;

import com.example.boardinghouse.repository.IdempotencyRecordRepository;
import com.example.boardinghouse.domain.entity.ServicePrice;
import com.example.boardinghouse.security.CustomUserDetailsService;
import com.example.boardinghouse.security.JwtAuthFilter;
import com.example.boardinghouse.service.ServicePriceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ServicePriceController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class ServicePriceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServicePriceService servicePriceService;

    @MockBean
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getServicePriceReturnsConfig() throws Exception {
        when(servicePriceService.getServicePrice("property-1")).thenReturn(servicePrice());

        mockMvc.perform(get("/api/properties/property-1/service-prices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.electricityPrice").value(3500))
                .andExpect(jsonPath("$.data.waterPrice").value(15000));
    }

    @Test
    void updateServicePriceReturnsUpdatedConfig() throws Exception {
        ServicePrice servicePrice = servicePrice();
        servicePrice.setElectricityPrice(4000L);
        servicePrice.setWaterPrice(20000L);
        when(servicePriceService.updateServicePrice(eq("property-1"), any())).thenReturn(servicePrice);

        mockMvc.perform(put("/api/properties/property-1/service-prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "electricityPrice": 4000,
                                  "waterPrice": 20000,
                                  "wifiFee": 100000,
                                  "garbageFee": 50000,
                                  "parkingFee": 150000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Service price updated successfully"))
                .andExpect(jsonPath("$.data.electricityPrice").value(4000));
    }

    @Test
    void updateServicePriceRejectsNegativeValue() throws Exception {
        mockMvc.perform(put("/api/properties/property-1/service-prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "electricityPrice": -1,
                                  "waterPrice": 20000,
                                  "wifiFee": 100000,
                                  "garbageFee": 50000,
                                  "parkingFee": 150000
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private ServicePrice servicePrice() {
        return ServicePrice.builder()
                .id("service-price-1")
                .propertyId("property-1")
                .electricityPrice(3500L)
                .waterPrice(15000L)
                .wifiFee(0L)
                .garbageFee(0L)
                .parkingFee(0L)
                .build();
    }
}

