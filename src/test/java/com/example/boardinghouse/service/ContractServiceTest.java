package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.domain.entity.Contract;
import com.example.boardinghouse.domain.entity.Room;
import com.example.boardinghouse.domain.entity.Tenant;
import com.example.boardinghouse.domain.enums.ContractStatus;
import com.example.boardinghouse.domain.enums.RoomStatus;
import com.example.boardinghouse.domain.enums.TenantStatus;
import com.example.boardinghouse.dto.contract.CreateContractRequest;
import com.example.boardinghouse.dto.contract.RenewContractRequest;
import com.example.boardinghouse.dto.contract.TerminateContractRequest;
import com.example.boardinghouse.dto.contract.UpdateContractRequest;
import com.example.boardinghouse.repository.ContractRepository;
import com.example.boardinghouse.repository.RoomRepository;
import com.example.boardinghouse.repository.TenantRepository;
import com.example.boardinghouse.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ContractService contractService;

    @BeforeEach
    void setUpOwner() {
        lenient().when(currentUserService.getOwnerId()).thenReturn("owner-1");
    }

    @Test
    void createContractUpdatesRoomAndTenantState() {
        Room room = room();
        Tenant tenant = tenant();
        CreateContractRequest request = createContractRequest();

        when(roomRepository.findByIdAndOwnerId("room-1", "owner-1")).thenReturn(Optional.of(room));
        when(tenantRepository.findByIdAndOwnerId("tenant-1", "owner-1")).thenReturn(Optional.of(tenant));
        when(contractRepository.findByRoomIdAndOwnerIdAndStatus("room-1", "owner-1", ContractStatus.ACTIVE)).thenReturn(Optional.empty());
        when(contractRepository.findByOwnerId("owner-1")).thenReturn(List.of());
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Contract contract = contractService.createContract(request);

        assertThat(contract.getOwnerId()).isEqualTo("owner-1");
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(contract.getRoomId()).isEqualTo("room-1");
        assertThat(room.getStatus()).isEqualTo(RoomStatus.OCCUPIED);
        assertThat(tenant.getCurrentRoomId()).isEqualTo("room-1");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        verify(roomRepository).save(room);
        verify(tenantRepository).saveAll(List.of(tenant));
    }

    @Test
    void createContractRejectsSecondActiveContractForRoom() {
        Room room = room();
        Tenant tenant = tenant();
        CreateContractRequest request = createContractRequest();
        Contract existingContract = Contract.builder()
                .id("contract-1")
                .ownerId("owner-1")
                .roomId("room-1")
                .status(ContractStatus.ACTIVE)
                .build();

        when(roomRepository.findByIdAndOwnerId("room-1", "owner-1")).thenReturn(Optional.of(room));
        when(tenantRepository.findByIdAndOwnerId("tenant-1", "owner-1")).thenReturn(Optional.of(tenant));
        when(contractRepository.findByRoomIdAndOwnerIdAndStatus("room-1", "owner-1", ContractStatus.ACTIVE))
                .thenReturn(Optional.of(existingContract));

        assertThatThrownBy(() -> contractService.createContract(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Room already has an active contract");

        verify(contractRepository, never()).save(any(Contract.class));
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void terminateContractUpdatesContractRoomAndTenantState() {
        Contract contract = activeContract();
        Room room = Room.builder()
                .id("room-1")
                .ownerId("owner-1")
                .status(RoomStatus.OCCUPIED)
                .build();
        Tenant tenant = Tenant.builder()
                .id("tenant-1")
                .ownerId("owner-1")
                .currentRoomId("room-1")
                .status(TenantStatus.ACTIVE)
                .build();
        TerminateContractRequest request = new TerminateContractRequest();
        request.setRoomStatus(RoomStatus.MAINTENANCE);

        when(contractRepository.findByIdAndOwnerId("contract-1", "owner-1")).thenReturn(Optional.of(contract));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roomRepository.findByIdAndOwnerId("room-1", "owner-1")).thenReturn(Optional.of(room));
        when(tenantRepository.findByIdAndOwnerId("tenant-1", "owner-1")).thenReturn(Optional.of(tenant));

        Contract terminatedContract = contractService.terminateContract("contract-1", request);

        assertThat(terminatedContract.getStatus()).isEqualTo(ContractStatus.TERMINATED);
        assertThat(terminatedContract.getTerminatedAt()).isNotNull();
        assertThat(room.getStatus()).isEqualTo(RoomStatus.MAINTENANCE);
        assertThat(tenant.getCurrentRoomId()).isNull();
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.LEFT);
    }

    @Test
    void terminateContractRejectsNonActiveContract() {
        Contract contract = activeContract();
        contract.setStatus(ContractStatus.TERMINATED);

        when(contractRepository.findByIdAndOwnerId("contract-1", "owner-1")).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> contractService.terminateContract("contract-1", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only active contract can be terminated");
    }

    @Test
    void renewContractExtendsEndDate() {
        Contract contract = activeContract();
        RenewContractRequest request = new RenewContractRequest();
        request.setNewEndDate(LocalDate.of(2028, 6, 1));
        request.setMonthlyRent(3_000_000L);

        when(contractRepository.findByIdAndOwnerId("contract-1", "owner-1")).thenReturn(Optional.of(contract));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Contract renewedContract = contractService.renewContract("contract-1", request);

        assertThat(renewedContract.getEndDate()).isEqualTo(LocalDate.of(2028, 6, 1));
        assertThat(renewedContract.getMonthlyRent()).isEqualTo(3_000_000L);
    }

    @Test
    void updateContractRejectsInvalidDateRange() {
        UpdateContractRequest request = new UpdateContractRequest();
        request.setStartDate(LocalDate.of(2026, 12, 1));
        request.setEndDate(LocalDate.of(2026, 6, 1));
        request.setMonthlyRent(2_500_000L);
        request.setDeposit(2_500_000L);
        request.setPaymentDueDay(5);

        when(contractRepository.findByIdAndOwnerId("contract-1", "owner-1")).thenReturn(Optional.of(activeContract()));

        assertThatThrownBy(() -> contractService.updateContract("contract-1", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Start date must be before end date");
    }

    private CreateContractRequest createContractRequest() {
        CreateContractRequest request = new CreateContractRequest();
        request.setRoomId("room-1");
        request.setTenantIds(List.of("tenant-1"));
        request.setStartDate(LocalDate.of(2026, 6, 1));
        request.setEndDate(LocalDate.of(2027, 6, 1));
        request.setMonthlyRent(2_500_000L);
        request.setDeposit(2_500_000L);
        request.setPaymentDueDay(5);
        return request;
    }

    private Contract activeContract() {
        return Contract.builder()
                .id("contract-1")
                .ownerId("owner-1")
                .roomId("room-1")
                .tenantIds(List.of("tenant-1"))
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2027, 6, 1))
                .monthlyRent(2_500_000L)
                .deposit(2_500_000L)
                .paymentDueDay(5)
                .status(ContractStatus.ACTIVE)
                .build();
    }

    private Room room() {
        return Room.builder()
                .id("room-1")
                .ownerId("owner-1")
                .maxTenants(2)
                .status(RoomStatus.AVAILABLE)
                .build();
    }

    private Tenant tenant() {
        return Tenant.builder()
                .id("tenant-1")
                .ownerId("owner-1")
                .status(TenantStatus.LEFT)
                .build();
    }
}
