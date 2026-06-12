package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.domain.entity.Room;
import com.example.boardinghouse.domain.enums.RoomStatus;
import com.example.boardinghouse.dto.room.CreateRoomRequest;
import com.example.boardinghouse.dto.room.UpdateRoomRequest;
import com.example.boardinghouse.repository.PropertyRepository;
import com.example.boardinghouse.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final PropertyRepository propertyRepository;
    private final MongoTemplate mongoTemplate;

    public List<Room> getRoomsByPropertyId(String propertyId) {
        ensurePropertyExists(propertyId);
        return roomRepository.findByPropertyId(propertyId);
    }

    public Room createRoom(String propertyId, CreateRoomRequest request) {
        ensurePropertyExists(propertyId);
        ensureRoomNumberAvailable(propertyId, request.getRoomNumber(), null);

        Room room = Room.builder()
                .propertyId(propertyId)
                .roomNumber(request.getRoomNumber())
                .floor(request.getFloor())
                .area(request.getArea())
                .baseRent(request.getBaseRent())
                .deposit(request.getDeposit())
                .maxTenants(request.getMaxTenants())
                .status(request.getStatus() == null ? RoomStatus.AVAILABLE : request.getStatus())
                .note(request.getNote())
                .build();

        return roomRepository.save(room);
    }

    public Room getRoomById(String id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
    }

    public Room updateRoom(String id, UpdateRoomRequest request) {
        Room room = getRoomById(id);
        ensureRoomNumberAvailable(room.getPropertyId(), request.getRoomNumber(), id);

        room.setRoomNumber(request.getRoomNumber());
        room.setFloor(request.getFloor());
        room.setArea(request.getArea());
        room.setBaseRent(request.getBaseRent());
        room.setDeposit(request.getDeposit());
        room.setMaxTenants(request.getMaxTenants());
        room.setStatus(request.getStatus() == null ? room.getStatus() : request.getStatus());
        room.setNote(request.getNote());

        return roomRepository.save(room);
    }

    public Room updateRoomStatus(String id, RoomStatus status) {
        Room room = getRoomById(id);
        room.setStatus(status);
        return roomRepository.save(room);
    }

    public void deleteRoom(String id) {
        Room room = getRoomById(id);
        long activeContractCount = mongoTemplate.count(
                Query.query(Criteria.where("roomId").is(id).and("status").is("ACTIVE")),
                "contracts"
        );

        if (activeContractCount > 0) {
            throw new BadRequestException("Cannot delete room because it has an active contract");
        }

        roomRepository.delete(room);
    }

    private void ensurePropertyExists(String propertyId) {
        if (!propertyRepository.existsById(propertyId)) {
            throw new ResourceNotFoundException("Property not found with id: " + propertyId);
        }
    }

    private void ensureRoomNumberAvailable(String propertyId, String roomNumber, String currentRoomId) {
        roomRepository.findByPropertyIdAndRoomNumber(propertyId, roomNumber)
                .filter(room -> currentRoomId == null || !room.getId().equals(currentRoomId))
                .ifPresent(room -> {
                    throw new BadRequestException("Room number already exists in this property");
                });
    }
}
