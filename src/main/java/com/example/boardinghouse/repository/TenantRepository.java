package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.Tenant;
import com.example.boardinghouse.domain.enums.TenantStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository (DAO) để thao tác với collection Tenant (Khách thuê) trong MongoDB.
 */
public interface TenantRepository extends MongoRepository<Tenant, String> {
    Optional<Tenant> findByEmailAndDeletedNot(String email, Boolean deleted);
    @Query("{'ownerId': ?0, 'deleted': {$ne: true}}")
    List<Tenant> findByOwnerId(String ownerId);

    @Query("{'_id': ?0, 'ownerId': ?1, 'deleted': {$ne: true}}")
    Optional<Tenant> findByIdAndOwnerId(String id, String ownerId);

    @Query("{'currentRoomId': ?0, 'deleted': {$ne: true}}")
    List<Tenant> findByCurrentRoomId(String roomId);

    @Query("{'currentRoomId': ?0, 'ownerId': ?1, 'deleted': {$ne: true}}")
    List<Tenant> findByCurrentRoomIdAndOwnerId(String roomId, String ownerId);

    @Query("{'status': ?0, 'deleted': {$ne: true}}")
    List<Tenant> findByStatus(TenantStatus status);

    @Query("{'ownerId': ?0, 'status': ?1, 'deleted': {$ne: true}}")
    List<Tenant> findByOwnerIdAndStatus(String ownerId, TenantStatus status);
}
