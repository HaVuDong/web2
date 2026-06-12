package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.Room;
import com.example.boardinghouse.domain.enums.RoomStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends MongoRepository<Room, String> {
    List<Room> findByPropertyId(String propertyId);

    Optional<Room> findByPropertyIdAndRoomNumber(String propertyId, String roomNumber);

    boolean existsByPropertyIdAndRoomNumber(String propertyId, String roomNumber);

    long countByStatus(RoomStatus status);
}
