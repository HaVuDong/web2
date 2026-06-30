package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.Room;
import com.example.boardinghouse.domain.enums.RoomStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository (DAO) để thao tác với collection Room (Phòng trọ) trong MongoDB.
 */
public interface RoomRepository extends MongoRepository<Room, String> {
    @Query("{'ownerId': ?0, 'deleted': {$ne: true}}")
    List<Room> findByOwnerId(String ownerId);

    @Query("{'_id': ?0, 'ownerId': ?1, 'deleted': {$ne: true}}")
    Optional<Room> findByIdAndOwnerId(String id, String ownerId);

    @Query("{'propertyId': ?0, 'deleted': {$ne: true}}")
    List<Room> findByPropertyId(String propertyId);

    @Query("{'propertyId': ?0, 'ownerId': ?1, 'deleted': {$ne: true}}")
    List<Room> findByPropertyIdAndOwnerId(String propertyId, String ownerId);

    Optional<Room> findByPropertyIdAndRoomNumber(String propertyId, String roomNumber);

    @Query("{'propertyId': ?0, 'roomNumber': ?1, 'ownerId': ?2, 'deleted': {$ne: true}}")
    Optional<Room> findByPropertyIdAndRoomNumberAndOwnerId(String propertyId, String roomNumber, String ownerId);

    boolean existsByPropertyIdAndRoomNumber(String propertyId, String roomNumber);

    @Query(value = "{'ownerId': ?0, 'deleted': {$ne: true}}", count = true)
    long countByOwnerId(String ownerId);

    long countByStatus(RoomStatus status);

    @Query(value = "{'ownerId': ?0, 'status': ?1, 'deleted': {$ne: true}}", count = true)
    long countByOwnerIdAndStatus(String ownerId, RoomStatus status);
}
