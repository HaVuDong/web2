package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.MeterReading;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository (DAO) để thao tác với collection MeterReading (Chỉ số điện nước) trong MongoDB.
 */
public interface MeterReadingRepository extends MongoRepository<MeterReading, String> {
    Optional<MeterReading> findByRoomIdAndMonthAndYear(String roomId, Integer month, Integer year);

    List<MeterReading> findByRoomIdOrderByYearDescMonthDesc(String roomId);

    boolean existsByRoomIdAndMonthAndYear(String roomId, Integer month, Integer year);
}
