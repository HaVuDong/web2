package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.MeterReading;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MeterReadingRepository extends MongoRepository<MeterReading, String> {
    Optional<MeterReading> findByRoomIdAndMonthAndYear(String roomId, Integer month, Integer year);

    List<MeterReading> findByRoomIdOrderByYearDescMonthDesc(String roomId);

    boolean existsByRoomIdAndMonthAndYear(String roomId, Integer month, Integer year);
}
