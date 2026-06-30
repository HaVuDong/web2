package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.MaintenanceRequest;
import com.example.boardinghouse.domain.enums.MaintenanceStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository (DAO) để thao tác với collection MaintenanceRequest (Yêu cầu bảo trì/sửa chữa) trong MongoDB.
 */
public interface MaintenanceRepository extends MongoRepository<MaintenanceRequest, String> {
    @Query("{'ownerId': ?0, 'deleted': {$ne: true}}")
    List<MaintenanceRequest> findByOwnerId(String ownerId);

    @Query("{'_id': ?0, 'ownerId': ?1, 'deleted': {$ne: true}}")
    Optional<MaintenanceRequest> findByIdAndOwnerId(String id, String ownerId);

    @Query("{'roomId': ?0, 'deleted': {$ne: true}}")
    List<MaintenanceRequest> findByRoomId(String roomId);

    @Query("{'status': ?0, 'deleted': {$ne: true}}")
    List<MaintenanceRequest> findByStatus(MaintenanceStatus status);

    @Query("{'ownerId': ?0, 'status': ?1, 'deleted': {$ne: true}}")
    List<MaintenanceRequest> findByOwnerIdAndStatus(String ownerId, MaintenanceStatus status);

    long countByStatus(MaintenanceStatus status);

    @Query(value = "{'ownerId': ?0, 'status': ?1, 'deleted': {$ne: true}}", count = true)
    long countByOwnerIdAndStatus(String ownerId, MaintenanceStatus status);
}
