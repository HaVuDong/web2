package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.Invoice;
import com.example.boardinghouse.domain.enums.InvoiceStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository (DAO) để thao tác với collection Invoice (Hóa đơn) trong MongoDB.
 */
public interface InvoiceRepository extends MongoRepository<Invoice, String> {
    @Query("{'ownerId': ?0, 'deleted': {$ne: true}}")
    List<Invoice> findByOwnerId(String ownerId);

    @Query("{'_id': ?0, 'ownerId': ?1, 'deleted': {$ne: true}}")
    Optional<Invoice> findByIdAndOwnerId(String id, String ownerId);

    @Query("{'roomId': ?0, 'month': ?1, 'year': ?2, 'deleted': {$ne: true}}")
    Optional<Invoice> findByRoomIdAndMonthAndYear(String roomId, Integer month, Integer year);

    @Query("{'roomId': ?0, 'ownerId': ?1, 'month': ?2, 'year': ?3, 'status': {$ne: 'CANCELLED'}, 'deleted': {$ne: true}}")
    Optional<Invoice> findActiveByRoomIdAndOwnerIdAndMonthAndYear(String roomId, String ownerId, Integer month, Integer year);

    @Query("{'roomId': ?0, 'deleted': {$ne: true}}")
    List<Invoice> findByRoomId(String roomId);

    @Query("{'roomId': ?0, 'ownerId': ?1, 'deleted': {$ne: true}}")
    List<Invoice> findByRoomIdAndOwnerId(String roomId, String ownerId);

    @Query("{'month': ?0, 'year': ?1, 'deleted': {$ne: true}}")
    List<Invoice> findByMonthAndYear(Integer month, Integer year);

    @Query("{'ownerId': ?0, 'month': ?1, 'year': ?2, 'deleted': {$ne: true}}")
    List<Invoice> findByOwnerIdAndMonthAndYear(String ownerId, Integer month, Integer year);

    @Query("{'status': ?0, 'deleted': {$ne: true}}")
    List<Invoice> findByStatus(InvoiceStatus status);

    @Query("{'ownerId': ?0, 'status': ?1, 'deleted': {$ne: true}}")
    List<Invoice> findByOwnerIdAndStatus(String ownerId, InvoiceStatus status);

    @Query("{'ownerId': ?0, 'status': {$in: ?1}, 'deleted': {$ne: true}}")
    List<Invoice> findByOwnerIdAndStatusIn(String ownerId, java.util.Set<InvoiceStatus> statuses);

    @Query(value = "{'ownerId': ?0, 'status': {$in: ?1}, 'deleted': {$ne: true}}", count = true)
    long countByOwnerIdAndStatusIn(String ownerId, java.util.Set<InvoiceStatus> statuses);
}
