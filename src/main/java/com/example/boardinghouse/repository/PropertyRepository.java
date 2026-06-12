package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.Property;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PropertyRepository extends MongoRepository<Property, String> {
}
