package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.ServicePrice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ServicePriceRepository extends MongoRepository<ServicePrice, String> {
    Optional<ServicePrice> findByPropertyId(String propertyId);
}
