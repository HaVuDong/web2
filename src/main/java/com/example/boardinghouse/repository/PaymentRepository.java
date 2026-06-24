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

    Optional<Payment> findFirstByInvoiceIdAndStatus(String invoiceId, PaymentStatus status);

    List<Payment> findByInvoiceId(String invoiceId);

    boolean existsByOrderCode(Long orderCode);
}
