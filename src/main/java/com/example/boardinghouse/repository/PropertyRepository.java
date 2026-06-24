package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.Property;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository (DAO) để thao tác với collection Property (Tòa nhà/Khu trọ) trong MongoDB.
 */
public interface PropertyRepository extends MongoRepository<Property, String> {
}
