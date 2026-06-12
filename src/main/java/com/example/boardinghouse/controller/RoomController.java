package com.example.boardinghouse.controller;

import com.example.boardinghouse.common.dto.ApiResponse;
import com.example.boardinghouse.domain.entity.Room;
import com.example.boardinghouse.dto.room.CreateRoomRequest;
import com.example.boardinghouse.dto.room.UpdateRoomRequest;
import com.example.boardinghouse.dto.room.UpdateRoomStatusRequest;
import com.example.boardinghouse.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/properties/{propertyId}/rooms")
    public ApiResponse<List<Room>> getRoomsByPropertyId(@PathVariable String propertyId) {
        return ApiResponse.success(roomService.getRoomsByPropertyId(propertyId));
    }

    @PostMapping("/properties/{propertyId}/rooms")
    public ApiResponse<Room> createRoom(
            @PathVariable String propertyId,
            @Valid @RequestBody CreateRoomRequest request
    ) {
        Room room = roomService.createRoom(propertyId, request);
        return ApiResponse.success("Room created successfully", room);
    }

    @GetMapping("/rooms/{id}")
    public ApiResponse<Room> getRoomById(@PathVariable String id) {
        return ApiResponse.success(roomService.getRoomById(id));
    }

    @PutMapping("/rooms/{id}")
    public ApiResponse<Room> updateRoom(
            @PathVariable String id,
            @Valid @RequestBody UpdateRoomRequest request
    ) {
        Room room = roomService.updateRoom(id, request);
        return ApiResponse.success("Room updated successfully", room);
    }

    @DeleteMapping("/rooms/{id}")
    public ApiResponse<Void> deleteRoom(@PathVariable String id) {
        roomService.deleteRoom(id);
        return ApiResponse.success("Room deleted successfully", null);
    }

    @PatchMapping("/rooms/{id}/status")
    public ApiResponse<Room> updateRoomStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateRoomStatusRequest request
    ) {
        Room room = roomService.updateRoomStatus(id, request.getStatus());
        return ApiResponse.success("Room status updated successfully", room);
    }
}
