package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.ServicePrice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Repository (DAO) để thao tác với collection ServicePrice (Bảng giá dịch vụ) trong MongoDB.
 */
public interface ServicePriceRepository extends MongoRepository<ServicePrice, String> {
    Optional<ServicePrice> findByPropertyId(String propertyId);

    Optional<ServicePrice> findByPropertyIdAndOwnerId(String propertyId, String ownerId);
}
