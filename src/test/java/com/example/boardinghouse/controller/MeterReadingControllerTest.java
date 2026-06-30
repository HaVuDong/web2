package com.example.boardinghouse.controller;

import com.example.boardinghouse.repository.IdempotencyRecordRepository;
import com.example.boardinghouse.domain.entity.MeterReading;
import com.example.boardinghouse.security.CustomUserDetailsService;
import com.example.boardinghouse.security.JwtAuthFilter;
import com.example.boardinghouse.service.MeterReadingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MeterReadingController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class MeterReadingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MeterReadingService meterReadingService;

    @MockBean
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void createMeterReadingReturnsCreatedReading() throws Exception {
        MeterReading meterReading = meterReading("meter-1", 6, 2026);
        when(meterReadingService.createMeterReading(any())).thenReturn(meterReading);

        mockMvc.perform(post("/api/meter-readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": "room-1",
                                  "month": 6,
                                  "year": 2026,
                                  "electricityOld": 100,
                                  "electricityNew": 120,
                                  "waterOld": 50,
                                  "waterNew": 60,
                                  "note": "Thang 6"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Meter reading created successfully"))
                .andExpect(jsonPath("$.data.month").value(6));
    }

    @Test
    void createMeterReadingRejectsInvalidMonth() throws Exception {
        mockMvc.perform(post("/api/meter-readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": "room-1",
                                  "month": 13,
                                  "year": 2026,
                                  "electricityOld": 100,
                                  "electricityNew": 120,
                                  "waterOld": 50,
                                  "waterNew": 60
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getMeterReadingsByRoomIdReturnsReadings() throws Exception {
        MeterReading meterReading = meterReading("meter-1", 6, 2026);
        when(meterReadingService.getMeterReadingsByRoomId("room-1")).thenReturn(List.of(meterReading));

        mockMvc.perform(get("/api/rooms/room-1/meter-readings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("meter-1"));
    }

    @Test
    void getLatestMeterReadingReturnsReading() throws Exception {
        MeterReading meterReading = meterReading("meter-2", 7, 2026);
        when(meterReadingService.getLatestMeterReading("room-1")).thenReturn(meterReading);

        mockMvc.perform(get("/api/rooms/room-1/latest-meter-reading"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.month").value(7));
    }

    private MeterReading meterReading(String id, Integer month, Integer year) {
        return MeterReading.builder()
                .id(id)
                .roomId("room-1")
                .month(month)
                .year(year)
                .electricityOld(100L)
                .electricityNew(120L)
                .waterOld(50L)
                .waterNew(60L)
                .build();
    }
}

