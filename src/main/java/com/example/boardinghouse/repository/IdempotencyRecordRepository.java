package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.IdempotencyRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface IdempotencyRecordRepository extends MongoRepository<IdempotencyRecord, String> {
    Optional<IdempotencyRecord> findByIdempotencyKeyAndOwnerId(String idempotencyKey, String ownerId);
    
    boolean existsByIdempotencyKeyAndOwnerId(String idempotencyKey, String ownerId);
}
