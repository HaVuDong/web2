package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.Tenant;
import com.example.boardinghouse.domain.enums.TenantStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Repository (DAO) để thao tác với collection Tenant (Khách thuê) trong MongoDB.
 */
public interface TenantRepository extends MongoRepository<Tenant, String> {
    List<Tenant> findByCurrentRoomId(String roomId);

    List<Tenant> findByStatus(TenantStatus status);
}
