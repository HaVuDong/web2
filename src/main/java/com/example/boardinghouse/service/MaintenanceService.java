package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.domain.entity.MaintenanceRequest;
import com.example.boardinghouse.domain.enums.MaintenanceStatus;
import com.example.boardinghouse.dto.maintenance.CreateMaintenanceRequest;
import com.example.boardinghouse.dto.maintenance.UpdateMaintenanceRequest;
import com.example.boardinghouse.repository.MaintenanceRepository;
import com.example.boardinghouse.repository.RoomRepository;
import com.example.boardinghouse.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;

    public List<MaintenanceRequest> getMaintenanceRequests(MaintenanceStatus status) {
        return status == null ? maintenanceRepository.findAll() : maintenanceRepository.findByStatus(status);
    }

    public MaintenanceRequest createMaintenanceRequest(CreateMaintenanceRequest request) {
        ensureRoomExists(request.getRoomId());
        ensureTenantExistsIfProvided(request.getTenantId());

        MaintenanceRequest maintenanceRequest = MaintenanceRequest.builder()
                .roomId(request.getRoomId())
                .tenantId(request.getTenantId())
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .status(MaintenanceStatus.PENDING)
                .build();

        return maintenanceRepository.save(maintenanceRequest);
    }

    public MaintenanceRequest getMaintenanceRequestById(String id) {
        return maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found with id: " + id));
    }

    public MaintenanceRequest updateMaintenanceRequest(String id, UpdateMaintenanceRequest request) {
        MaintenanceRequest maintenanceRequest = getMaintenanceRequestById(id);
        ensureTenantExistsIfProvided(request.getTenantId());

        maintenanceRequest.setTenantId(request.getTenantId());
        maintenanceRequest.setTitle(request.getTitle());
        maintenanceRequest.setDescription(request.getDescription());
        maintenanceRequest.setPriority(request.getPriority());

        return maintenanceRepository.save(maintenanceRequest);
    }

    public MaintenanceRequest updateMaintenanceStatus(String id, MaintenanceStatus status) {
        MaintenanceRequest maintenanceRequest = getMaintenanceRequestById(id);
        maintenanceRequest.setStatus(status);
        maintenanceRequest.setCompletedAt(status == MaintenanceStatus.DONE ? LocalDateTime.now() : null);
        return maintenanceRepository.save(maintenanceRequest);
    }

    public void deleteMaintenanceRequest(String id) {
        MaintenanceRequest maintenanceRequest = getMaintenanceRequestById(id);
        maintenanceRepository.delete(maintenanceRequest);
    }

    private void ensureRoomExists(String roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new ResourceNotFoundException("Room not found with id: " + roomId);
        }
    }

    private void ensureTenantExistsIfProvided(String tenantId) {
        if (tenantId != null && !tenantId.isBlank() && !tenantRepository.existsById(tenantId)) {
            throw new ResourceNotFoundException("Tenant not found with id: " + tenantId);
        }
    }
}
