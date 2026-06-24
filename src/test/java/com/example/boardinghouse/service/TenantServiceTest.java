package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.domain.entity.Tenant;
import com.example.boardinghouse.domain.enums.TenantStatus;
import com.example.boardinghouse.dto.tenant.CreateTenantRequest;
import com.example.boardinghouse.repository.RoomRepository;
import com.example.boardinghouse.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void createTenantCreatesActiveTenantByDefault() {
        CreateTenantRequest request = createTenantRequest();
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tenant tenant = tenantService.createTenant(request);

        assertThat(tenant.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(tenant.getCurrentRoomId()).isNull();
    }

    @Test
    void createTenantRejectsMissingCurrentRoom() {
        CreateTenantRequest request = createTenantRequest();
        request.setCurrentRoomId("missing-room");

        when(roomRepository.existsById("missing-room")).thenReturn(false);

        assertThatThrownBy(() -> tenantService.createTenant(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Room not found with id: missing-room");

        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    void getTenantsSearchesByKeywordAndStatus() {
        Tenant tenant = tenant("tenant-1", TenantStatus.ACTIVE, null);
        when(mongoTemplate.find(any(Query.class), eq(Tenant.class))).thenReturn(List.of(tenant));

        List<Tenant> tenants = tenantService.getTenants("nguyen", TenantStatus.ACTIVE);

        assertThat(tenants).containsExactly(tenant);
        verify(mongoTemplate).find(any(Query.class), eq(Tenant.class));
    }

    @Test
    void getTenantsByRoomIdRejectsMissingRoom() {
        when(roomRepository.existsById("missing-room")).thenReturn(false);

        assertThatThrownBy(() -> tenantService.getTenantsByRoomId("missing-room"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Room not found with id: missing-room");
    }

    @Test
    void markTenantLeftRejectsActiveContract() {
        Tenant tenant = tenant("tenant-1", TenantStatus.ACTIVE, "room-1");
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenant));
        when(mongoTemplate.count(any(Query.class), eq("contracts"))).thenReturn(1L);

        assertThatThrownBy(() -> tenantService.markTenantLeft("tenant-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot mark tenant as left because tenant has an active contract");

        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    void markTenantLeftClearsRoomWhenNoActiveContractExists() {
        Tenant tenant = tenant("tenant-1", TenantStatus.ACTIVE, "room-1");
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenant));
        when(mongoTemplate.count(any(Query.class), eq("contracts"))).thenReturn(0L);

        tenantService.markTenantLeft("tenant-1");

        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TenantStatus.LEFT);
        assertThat(captor.getValue().getCurrentRoomId()).isNull();
    }

    private CreateTenantRequest createTenantRequest() {
        CreateTenantRequest request = new CreateTenantRequest();
        request.setFullName("Nguyen Van A");
        request.setPhone("0909123456");
        request.setEmail("tenant@gmail.com");
        request.setIdentityNumber("123456789");
        request.setPermanentAddress("Ha Noi");
        request.setNote("Khach moi");
        return request;
    }

    private Tenant tenant(String id, TenantStatus status, String currentRoomId) {
        return Tenant.builder()
                .id(id)
                .fullName("Nguyen Van A")
                .phone("0909123456")
                .email("tenant@gmail.com")
                .status(status)
                .currentRoomId(currentRoomId)
                .build();
    }
}
