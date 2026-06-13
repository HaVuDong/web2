package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.MaintenanceRequest;
import com.example.boardinghouse.domain.enums.MaintenanceStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MaintenanceRepository extends MongoRepository<MaintenanceRequest, String> {
    List<MaintenanceRequest> findByRoomId(String roomId);

    List<MaintenanceRequest> findByStatus(MaintenanceStatus status);

    long countByStatus(MaintenanceStatus status);
}
