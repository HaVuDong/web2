package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.Contract;
import com.example.boardinghouse.domain.enums.ContractStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository (DAO) để thao tác với bảng (collection) Contract (Hợp đồng thuê phòng) trong MongoDB.
 */
public interface ContractRepository extends MongoRepository<Contract, String> {
    @Query("{'ownerId': ?0, 'deleted': {$ne: true}}")
    List<Contract> findByOwnerId(String ownerId);

    @Query("{'_id': ?0, 'ownerId': ?1, 'deleted': {$ne: true}}")
    Optional<Contract> findByIdAndOwnerId(String id, String ownerId);

    @Query("{'roomId': ?0, 'status': ?1, 'deleted': {$ne: true}}")
    Optional<Contract> findByRoomIdAndStatus(String roomId, ContractStatus status);

    @Query("{'roomId': ?0, 'ownerId': ?1, 'status': ?2, 'deleted': {$ne: true}}")
    Optional<Contract> findByRoomIdAndOwnerIdAndStatus(String roomId, String ownerId, ContractStatus status);

    @Query("{'status': ?0, 'deleted': {$ne: true}}")
    List<Contract> findByStatus(ContractStatus status);

    @Query(value = "{'roomId': ?0, 'ownerId': ?1, 'status': ?2, 'deleted': {$ne: true}}", exists = true)
    boolean existsByRoomIdAndOwnerIdAndStatus(String roomId, String ownerId, ContractStatus status);

    @Query(value = "{'ownerId': ?0, 'status': ?1, 'tenantIds': {$in: ?2}, 'deleted': {$ne: true}}", exists = true)
    boolean existsByOwnerIdAndStatusAndTenantIdsIn(String ownerId, ContractStatus status, List<String> tenantIds);
}
