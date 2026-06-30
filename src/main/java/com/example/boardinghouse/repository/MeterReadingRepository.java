package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.MeterReading;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository (DAO) để thao tác với collection MeterReading (Chỉ số điện nước) trong MongoDB.
 */
public interface MeterReadingRepository extends MongoRepository<MeterReading, String> {
    @Query("{'ownerId': ?0, 'deleted': {$ne: true}}")
    List<MeterReading> findByOwnerId(String ownerId);

    @Query("{'_id': ?0, 'ownerId': ?1, 'deleted': {$ne: true}}")
    Optional<MeterReading> findByIdAndOwnerId(String id, String ownerId);

    @Query("{'roomId': ?0, 'month': ?1, 'year': ?2, 'deleted': {$ne: true}}")
    Optional<MeterReading> findByRoomIdAndMonthAndYear(String roomId, Integer month, Integer year);

    @Query("{'roomId': ?0, 'ownerId': ?1, 'month': ?2, 'year': ?3, 'deleted': {$ne: true}}")
    Optional<MeterReading> findByRoomIdAndOwnerIdAndMonthAndYear(String roomId, String ownerId, Integer month, Integer year);

    @Query(value = "{'roomId': ?0, 'deleted': {$ne: true}}", sort = "{'year': -1, 'month': -1}")
    List<MeterReading> findByRoomIdOrderByYearDescMonthDesc(String roomId);

    @Query(value = "{'roomId': ?0, 'ownerId': ?1, 'deleted': {$ne: true}}", sort = "{'year': -1, 'month': -1}")
    List<MeterReading> findByRoomIdAndOwnerIdOrderByYearDescMonthDesc(String roomId, String ownerId);

    boolean existsByRoomIdAndMonthAndYear(String roomId, Integer month, Integer year);
}
