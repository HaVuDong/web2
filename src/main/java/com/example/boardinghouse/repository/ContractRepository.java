package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.Contract;
import com.example.boardinghouse.domain.enums.ContractStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository (DAO) để thao tác với bảng (collection) Contract (Hợp đồng thuê phòng) trong MongoDB.
 */
public interface ContractRepository extends MongoRepository<Contract, String> {
    Optional<Contract> findByRoomIdAndStatus(String roomId, ContractStatus status);

    List<Contract> findByStatus(ContractStatus status);
}
