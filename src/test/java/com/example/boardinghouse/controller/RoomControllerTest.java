package com.example.boardinghouse.controller;

import com.example.boardinghouse.repository.IdempotencyRecordRepository;
import com.example.boardinghouse.domain.entity.Room;
import com.example.boardinghouse.domain.enums.RoomStatus;
import com.example.boardinghouse.security.CustomUserDetailsService;
import com.example.boardinghouse.security.JwtAuthFilter;
import com.example.boardinghouse.service.RoomService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RoomController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomService roomService;

    @MockBean
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getRoomsByPropertyIdReturnsRooms() throws Exception {
        Room room = Room.builder()
                .id("room-1")
                .propertyId("property-1")
                .roomNumber("101")
                .status(RoomStatus.AVAILABLE)
                .build();

        when(roomService.getRoomsByPropertyId("property-1")).thenReturn(List.of(room));

        mockMvc.perform(get("/api/properties/property-1/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("room-1"))
                .andExpect(jsonPath("$.data[0].roomNumber").value("101"));
    }

    @Test
    void createRoomReturnsCreatedRoom() throws Exception {
        Room room = Room.builder()
                .id("room-1")
                .propertyId("property-1")
                .roomNumber("101")
                .status(RoomStatus.AVAILABLE)
                .build();

        when(roomService.createRoom(eq("property-1"), any())).thenReturn(room);

        mockMvc.perform(post("/api/properties/property-1/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomNumber": "101",
                                  "floor": 1,
                                  "area": 20,
                                  "baseRent": 2500000,
                                  "deposit": 2500000,
                                  "maxTenants": 2,
                                  "status": "AVAILABLE",
                                  "note": "Gan cau thang"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Room created successfully"))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"));
    }

    @Test
    void createRoomRejectsBlankRoomNumber() throws Exception {
        mockMvc.perform(post("/api/properties/property-1/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomNumber": "",
                                  "floor": 1,
                                  "maxTenants": 2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateRoomStatusReturnsUpdatedRoom() throws Exception {
        Room room = Room.builder()
                .id("room-1")
                .propertyId("property-1")
                .roomNumber("101")
                .status(RoomStatus.MAINTENANCE)
                .build();

        when(roomService.updateRoomStatus("room-1", RoomStatus.MAINTENANCE)).thenReturn(room);

        mockMvc.perform(patch("/api/rooms/room-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "MAINTENANCE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Room status updated successfully"))
                .andExpect(jsonPath("$.data.status").value("MAINTENANCE"));
    }

    @Test
    void updateRoomStatusRejectsInvalidStatus() throws Exception {
        mockMvc.perform(patch("/api/rooms/room-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "BROKEN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid request body"));
    }
}

