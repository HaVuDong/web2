package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.domain.entity.MaintenanceRequest;
import com.example.boardinghouse.domain.enums.MaintenancePriority;
import com.example.boardinghouse.domain.enums.MaintenanceStatus;
import com.example.boardinghouse.dto.maintenance.CreateMaintenanceRequest;
import com.example.boardinghouse.dto.maintenance.UpdateMaintenanceRequest;
import com.example.boardinghouse.repository.MaintenanceRepository;
import com.example.boardinghouse.repository.RoomRepository;
import com.example.boardinghouse.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceTest {

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private MaintenanceService maintenanceService;

    @Test
    void createMaintenanceRequestCreatesPendingRequest() {
        when(roomRepository.existsById("room-1")).thenReturn(true);
        when(tenantRepository.existsById("tenant-1")).thenReturn(true);
        when(maintenanceRepository.save(any(MaintenanceRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MaintenanceRequest maintenanceRequest = maintenanceService.createMaintenanceRequest(createRequest());

        assertThat(maintenanceRequest.getRoomId()).isEqualTo("room-1");
        assertThat(maintenanceRequest.getTenantId()).isEqualTo("tenant-1");
        assertThat(maintenanceRequest.getTitle()).isEqualTo("Fix water leak");
        assertThat(maintenanceRequest.getPriority()).isEqualTo(MaintenancePriority.HIGH);
        assertThat(maintenanceRequest.getStatus()).isEqualTo(MaintenanceStatus.PENDING);
        assertThat(maintenanceRequest.getCompletedAt()).isNull();
    }

    @Test
    void createMaintenanceRequestRejectsMissingRoom() {
        when(roomRepository.existsById("room-1")).thenReturn(false);

        assertThatThrownBy(() -> maintenanceService.createMaintenanceRequest(createRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Room not found with id: room-1");
    }

    @Test
    void updateMaintenanceStatusDoneSetsCompletedAt() {
        MaintenanceRequest maintenanceRequest = maintenanceRequest(MaintenanceStatus.IN_PROGRESS);
        when(maintenanceRepository.findById("maintenance-1")).thenReturn(Optional.of(maintenanceRequest));
        when(maintenanceRepository.save(any(MaintenanceRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MaintenanceRequest updated = maintenanceService.updateMaintenanceStatus("maintenance-1", MaintenanceStatus.DONE);

        assertThat(updated.getStatus()).isEqualTo(MaintenanceStatus.DONE);
        assertThat(updated.getCompletedAt()).isNotNull();
    }

    @Test
    void getMaintenanceRequestsFiltersByStatus() {
        when(maintenanceRepository.findByStatus(MaintenanceStatus.PENDING))
                .thenReturn(List.of(maintenanceRequest(MaintenanceStatus.PENDING)));

        List<MaintenanceRequest> requests = maintenanceService.getMaintenanceRequests(MaintenanceStatus.PENDING);

        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getStatus()).isEqualTo(MaintenanceStatus.PENDING);
    }

    @Test
    void updateMaintenanceRequestUpdatesEditableFields() {
        MaintenanceRequest maintenanceRequest = maintenanceRequest(MaintenanceStatus.PENDING);
        UpdateMaintenanceRequest request = new UpdateMaintenanceRequest();
        request.setTenantId("tenant-2");
        request.setTitle("Replace light");
        request.setDescription("Broken hallway light");
        request.setPriority(MaintenancePriority.MEDIUM);

        when(maintenanceRepository.findById("maintenance-1")).thenReturn(Optional.of(maintenanceRequest));
        when(tenantRepository.existsById("tenant-2")).thenReturn(true);
        when(maintenanceRepository.save(any(MaintenanceRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MaintenanceRequest updated = maintenanceService.updateMaintenanceRequest("maintenance-1", request);

        assertThat(updated.getTenantId()).isEqualTo("tenant-2");
        assertThat(updated.getTitle()).isEqualTo("Replace light");
        assertThat(updated.getPriority()).isEqualTo(MaintenancePriority.MEDIUM);

        ArgumentCaptor<MaintenanceRequest> captor = ArgumentCaptor.forClass(MaintenanceRequest.class);
        verify(maintenanceRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("Broken hallway light");
    }

    private CreateMaintenanceRequest createRequest() {
        CreateMaintenanceRequest request = new CreateMaintenanceRequest();
        request.setRoomId("room-1");
        request.setTenantId("tenant-1");
        request.setTitle("Fix water leak");
        request.setDescription("Sink is leaking");
        request.setPriority(MaintenancePriority.HIGH);
        return request;
    }

    private MaintenanceRequest maintenanceRequest(MaintenanceStatus status) {
        return MaintenanceRequest.builder()
                .id("maintenance-1")
                .roomId("room-1")
                .tenantId("tenant-1")
                .title("Fix water leak")
                .description("Sink is leaking")
                .priority(MaintenancePriority.HIGH)
                .status(status)
                .build();
    }
}
