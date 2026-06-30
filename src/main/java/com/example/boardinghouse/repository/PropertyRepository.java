package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.Property;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository (DAO) để thao tác với collection Property (Tòa nhà/Khu trọ) trong MongoDB.
 */
public interface PropertyRepository extends MongoRepository<Property, String> {
    @Query("{'ownerId': ?0, 'deleted': {$ne: true}}")
    List<Property> findByOwnerId(String ownerId);

    @Query("{'_id': ?0, 'ownerId': ?1, 'deleted': {$ne: true}}")
    Optional<Property> findByIdAndOwnerId(String id, String ownerId);
}
