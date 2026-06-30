package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.Payment;
import com.example.boardinghouse.domain.enums.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository (DAO) để thao tác với collection Payment (Lịch sử thanh toán) trong MongoDB.
 */
public interface PaymentRepository extends MongoRepository<Payment, String> {
    Optional<Payment> findByOrderCode(Long orderCode);

    Optional<Payment> findByIdAndOwnerId(String id, String ownerId);

    Optional<Payment> findFirstByInvoiceIdAndStatus(String invoiceId, PaymentStatus status);

    Optional<Payment> findFirstByInvoiceIdAndOwnerIdAndStatus(String invoiceId, String ownerId, PaymentStatus status);

    List<Payment> findByInvoiceId(String invoiceId);

    List<Payment> findByInvoiceIdAndOwnerId(String invoiceId, String ownerId);

    List<Payment> findByInvoiceIdAndOwnerIdAndStatus(String invoiceId, String ownerId, PaymentStatus status);

    boolean existsByOrderCode(Long orderCode);
}
