package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.Invoice;
import com.example.boardinghouse.domain.enums.InvoiceStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository (DAO) để thao tác với collection Invoice (Hóa đơn) trong MongoDB.
 */
public interface InvoiceRepository extends MongoRepository<Invoice, String> {
    Optional<Invoice> findByRoomIdAndMonthAndYear(String roomId, Integer month, Integer year);

    List<Invoice> findByRoomId(String roomId);

    List<Invoice> findByMonthAndYear(Integer month, Integer year);

    List<Invoice> findByStatus(InvoiceStatus status);
}
