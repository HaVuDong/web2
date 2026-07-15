package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.domain.entity.Contract;
import com.example.boardinghouse.domain.entity.Room;
import com.example.boardinghouse.domain.entity.Tenant;
import com.example.boardinghouse.domain.enums.ContractStatus;
import com.example.boardinghouse.domain.enums.RoomStatus;
import com.example.boardinghouse.domain.enums.TenantStatus;
import com.example.boardinghouse.dto.contract.CreateContractRequest;
import com.example.boardinghouse.dto.contract.RenewContractRequest;
import com.example.boardinghouse.dto.contract.SwitchRoomRequest;
import com.example.boardinghouse.dto.contract.TerminateContractRequest;
import com.example.boardinghouse.dto.contract.UpdateContractRequest;
import com.example.boardinghouse.dto.contract.UpdateContractTenantsRequest;
import com.example.boardinghouse.repository.ContractRepository;
import com.example.boardinghouse.repository.RoomRepository;
import com.example.boardinghouse.repository.TenantRepository;
import com.example.boardinghouse.security.CurrentUserService;
import com.example.boardinghouse.realtime.RealtimeEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    /**
     * Lấy danh sách toàn bộ hợp đồng thuê phòng.
     *
     * @return Danh sách các hợp đồng
     */
    public List<Contract> getAllContracts() {
        return contractRepository.findByOwnerId(currentUserService.getOwnerId());
    }

    /**
     * Tạo mới một hợp đồng thuê phòng.
     * Kiểm tra phòng, khách thuê, tính hợp lệ của ngày bắt đầu/kết thúc.
     * Đảm bảo phòng này chưa có hợp đồng nào đang ACTIVE.
     * Sau khi tạo hợp đồng, cập nhật trạng thái phòng thành OCCUPIED (Đã có người)
     * và cập nhật trạng thái khách thuê thành ACTIVE.
     *
     * @param request Dữ liệu tạo hợp đồng
     * @return Hợp đồng mới được lưu
     */
    public Contract createContract(CreateContractRequest request) {
        String ownerId = currentUserService.getOwnerId();
        Room room = getRoom(request.getRoomId(), ownerId);
        List<String> tenantIds = normalizeTenantIds(request.getTenantIds());
        List<Tenant> tenants = getTenants(tenantIds, ownerId);
        validateDateRange(request.getStartDate(), request.getEndDate());
        validateRoomForNewContract(room);
        validateRoomCapacity(room, tenantIds);
        ensureRoomHasNoActiveContract(room.getId(), ownerId);
        ensureTenantsHaveNoActiveContract(tenantIds, ownerId);

        Contract contract = Contract.builder()
                .ownerId(ownerId)
                .roomId(room.getId())
                .tenantIds(tenantIds)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .monthlyRent(request.getMonthlyRent())
                .deposit(request.getDeposit())
                .paymentDueDay(request.getPaymentDueDay())
                .status(ContractStatus.ACTIVE)
                .note(request.getNote())
                .build();

        Contract savedContract = contractRepository.save(contract);
        auditService.log("CREATE", "CONTRACT", savedContract.getId(), null, savedContract);

        try {
            room.setStatus(RoomStatus.OCCUPIED);
            roomRepository.save(room);

            tenants.forEach(tenant -> {
                tenant.setCurrentRoomId(room.getId());
                tenant.setStatus(TenantStatus.ACTIVE);
            });
            tenantRepository.saveAll(tenants);
        } catch (Exception e) {
            contractRepository.deleteById(savedContract.getId());
            throw new RuntimeException("Failed to save room or tenants during contract creation, rolled back contract.", e);
        }

        realtimeEventPublisher.publishGlobalUpdate();
        return savedContract;
    }

    /**
     * Lấy thông tin hợp đồng theo ID.
     * Nếu không tìm thấy sẽ ném ngoại lệ.
     *
     * @param id ID hợp đồng
     * @return Hợp đồng
     */
    public Contract getContractById(String id) {
        return contractRepository.findByIdAndOwnerId(id, currentUserService.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found with id: " + id));
    }

    /**
     * Cập nhật thông tin của hợp đồng đang có.
     * Không cho phép cập nhật nếu hợp đồng đã bị chấm dứt (TERMINATED).
     *
     * @param id ID của hợp đồng cần cập nhật
     * @param request Dữ liệu cập nhật
     * @return Hợp đồng đã được cập nhật
     */
    public Contract updateContract(String id, UpdateContractRequest request) {
        Contract contract = getContractById(id);
        if (contract.getStatus() == ContractStatus.TERMINATED) {
            throw new BadRequestException("Cannot update terminated contract");
        }

        validateDateRange(request.getStartDate(), request.getEndDate());
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setMonthlyRent(request.getMonthlyRent());
        contract.setDeposit(request.getDeposit());
        contract.setPaymentDueDay(request.getPaymentDueDay());
        contract.setNote(request.getNote());

        Contract saved = contractRepository.save(contract);
        auditService.log("UPDATE", "CONTRACT", saved.getId(), null, saved);
        realtimeEventPublisher.publishGlobalUpdate();
        return saved;
    }

    /**
     * Chấm dứt hợp đồng thuê phòng.
     * Chỉ được chấm dứt các hợp đồng đang ở trạng thái ACTIVE.
     * Sau khi chấm dứt, cập nhật trạng thái phòng (ví dụ: AVAILABLE hoặc MAINTENANCE)
     * và đổi trạng thái của tất cả khách thuê trong hợp đồng thành LEFT (Đã rời đi).
     *
     * @param id ID hợp đồng cần chấm dứt
     * @param request Dữ liệu yêu cầu chấm dứt
     * @return Hợp đồng sau khi cập nhật trạng thái TERMINATED
     */
    public Contract terminateContract(String id, TerminateContractRequest request) {
        Contract contract = getContractById(id);
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BadRequestException("Only active contract can be terminated");
        }

        RoomStatus roomStatus = resolveTerminateRoomStatus(request);
        contract.setStatus(ContractStatus.TERMINATED);
        contract.setTerminatedAt(LocalDateTime.now());
        if (request != null && request.getNote() != null) {
            contract.setNote(request.getNote());
        }
        Contract savedContract = contractRepository.save(contract);
        auditService.log("TERMINATE", "CONTRACT", savedContract.getId(), null, savedContract);

        roomRepository.findByIdAndOwnerId(contract.getRoomId(), contract.getOwnerId()).ifPresent(room -> {
            room.setStatus(roomStatus);
            roomRepository.save(room);
        });

        List<Tenant> tenants = contract.getTenantIds().stream()
                .map(tenantId -> tenantRepository.findByIdAndOwnerId(tenantId, contract.getOwnerId()))
                .flatMap(Optional::stream)
                .toList();
        tenants.forEach(tenant -> {
            tenant.setCurrentRoomId(null);
            tenant.setStatus(TenantStatus.LEFT);
        });
        tenantRepository.saveAll(tenants);

        realtimeEventPublisher.publishGlobalUpdate();
        return savedContract;
    }

    /**
     * Gia hạn hợp đồng thuê phòng hiện tại.
     * Chỉ áp dụng cho hợp đồng đang ACTIVE. Ngày kết thúc mới phải sau ngày kết thúc cũ.
     * Có thể cập nhật giá thuê, tiền cọc mới nếu cần.
     *
     * @param id ID hợp đồng
     * @param request Thông tin gia hạn
     * @return Hợp đồng sau khi gia hạn
     */
    public Contract renewContract(String id, RenewContractRequest request) {
        Contract contract = getContractById(id);
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BadRequestException("Only active contract can be renewed");
        }

        if (!request.getNewEndDate().isAfter(contract.getEndDate())) {
            throw new BadRequestException("New end date must be after current end date");
        }

        contract.setEndDate(request.getNewEndDate());
        if (request.getMonthlyRent() != null) {
            contract.setMonthlyRent(request.getMonthlyRent());
        }
        if (request.getDeposit() != null) {
            contract.setDeposit(request.getDeposit());
        }
        if (request.getPaymentDueDay() != null) {
            contract.setPaymentDueDay(request.getPaymentDueDay());
        }
        if (request.getNote() != null) {
            contract.setNote(request.getNote());
        }

        Contract saved = contractRepository.save(contract);
        auditService.log("RENEW", "CONTRACT", saved.getId(), null, saved);
        realtimeEventPublisher.publishGlobalUpdate();
        return saved;
    }

    /**
     * Đổi phòng cho hợp đồng đang hoạt động.
     * Chỉ áp dụng cho hợp đồng ACTIVE. Phòng mới phải ở trạng thái AVAILABLE.
     * Sau khi đổi:
     *  - Phòng cũ → AVAILABLE
     *  - Phòng mới → OCCUPIED
     *  - Cập nhật roomId trên hợp đồng
     *  - Cập nhật currentRoomId trên tất cả khách thuê trong hợp đồng
     *
     * @param id ID hợp đồng
     * @param request Thông tin đổi phòng (newRoomId, note)
     * @return Hợp đồng sau khi đổi phòng
     */
    public Contract switchRoom(String id, SwitchRoomRequest request) {
        String ownerId = currentUserService.getOwnerId();
        Contract contract = getContractById(id);

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BadRequestException("Only active contract can switch room");
        }

        Room oldRoom = getRoom(contract.getRoomId(), ownerId);
        Room newRoom = getRoom(request.getNewRoomId(), ownerId);

        if (oldRoom.getId().equals(newRoom.getId())) {
            throw new BadRequestException("New room must be different from current room");
        }

        if (newRoom.getStatus() != RoomStatus.AVAILABLE) {
            throw new BadRequestException("New room must be available to switch");
        }

        ensureRoomHasNoActiveContract(newRoom.getId(), ownerId);
        validateRoomCapacity(newRoom, contract.getTenantIds());

        String oldRoomId = contract.getRoomId();
        contract.setRoomId(newRoom.getId());
        if (request.getNote() != null) {
            contract.setNote(request.getNote());
        }

        Contract savedContract = contractRepository.save(contract);
        auditService.log("SWITCH_ROOM", "CONTRACT", savedContract.getId(), null, savedContract);

        try {
            oldRoom.setStatus(RoomStatus.AVAILABLE);
            roomRepository.save(oldRoom);

            newRoom.setStatus(RoomStatus.OCCUPIED);
            roomRepository.save(newRoom);

            List<Tenant> tenants = contract.getTenantIds().stream()
                    .map(tenantId -> tenantRepository.findByIdAndOwnerId(tenantId, ownerId))
                    .flatMap(Optional::stream)
                    .toList();
            tenants.forEach(tenant -> tenant.setCurrentRoomId(newRoom.getId()));
            tenantRepository.saveAll(tenants);
        } catch (Exception e) {
            contract.setRoomId(oldRoomId);
            contractRepository.save(contract);
            throw new RuntimeException("Failed to switch room, rolled back contract.", e);
        }

        realtimeEventPublisher.publishGlobalUpdate();
        return savedContract;
    }

    /**
     * Cập nhật danh sách khách thuê của hợp đồng đang hoạt động.
     * Thêm khách mới vào phòng hoặc xóa khách cũ khỏi phòng.
     *
     * @param id ID hợp đồng
     * @param request Dữ liệu cập nhật khách thuê
     * @return Hợp đồng sau khi cập nhật
     */
    public Contract updateTenants(String id, UpdateContractTenantsRequest request) {
        String ownerId = currentUserService.getOwnerId();
        Contract contract = getContractById(id);

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BadRequestException("Only active contract can update tenants");
        }

        List<String> newTenantIds = normalizeTenantIds(request.getTenantIds());
        List<String> oldTenantIds = contract.getTenantIds();

        Room room = getRoom(contract.getRoomId(), ownerId);
        validateRoomCapacity(room, newTenantIds);

        List<String> addedTenantIds = newTenantIds.stream()
                .filter(tenantId -> !oldTenantIds.contains(tenantId))
                .toList();
        List<String> removedTenantIds = oldTenantIds.stream()
                .filter(tenantId -> !newTenantIds.contains(tenantId))
                .toList();

        if (!addedTenantIds.isEmpty()) {
            ensureTenantsHaveNoActiveContract(addedTenantIds, ownerId);
        }

        List<Tenant> allTenantsToSave = new java.util.ArrayList<>();

        if (!removedTenantIds.isEmpty()) {
            List<Tenant> removedTenants = getTenants(removedTenantIds, ownerId);
            removedTenants.forEach(t -> {
                t.setCurrentRoomId(null);
                t.setStatus(TenantStatus.LEFT);
            });
            allTenantsToSave.addAll(removedTenants);
        }

        if (!addedTenantIds.isEmpty()) {
            List<Tenant> addedTenants = getTenants(addedTenantIds, ownerId);
            addedTenants.forEach(t -> {
                t.setCurrentRoomId(room.getId());
                t.setStatus(TenantStatus.ACTIVE);
            });
            allTenantsToSave.addAll(addedTenants);
        }

        contract.setTenantIds(newTenantIds);
        Contract savedContract = contractRepository.save(contract);
        auditService.log("UPDATE_TENANTS", "CONTRACT", savedContract.getId(), null, savedContract);

        if (!allTenantsToSave.isEmpty()) {
            tenantRepository.saveAll(allTenantsToSave);
        }

        realtimeEventPublisher.publishGlobalUpdate();
        return savedContract;
    }

    /**
     * Lấy thông tin phòng dựa theo ID, ném ngoại lệ nếu không có.
     */
    private Room getRoom(String roomId, String ownerId) {
        return roomRepository.findByIdAndOwnerId(roomId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));
    }

    /**
     * Lấy danh sách khách thuê theo ID, đảm bảo tất cả ID đều hợp lệ.
     */
    private List<Tenant> getTenants(List<String> tenantIds, String ownerId) {
        List<Tenant> tenants = tenantIds.stream()
                .map(tenantId -> tenantRepository.findByIdAndOwnerId(tenantId, ownerId)
                        .orElseThrow(() -> new ResourceNotFoundException("One or more tenants were not found")))
                .toList();
        if (tenants.size() != tenantIds.size()) {
            throw new ResourceNotFoundException("One or more tenants were not found");
        }

        return tenants;
    }

    /**
     * Lọc và loại bỏ các ID khách thuê bị trùng lặp trong request.
     */
    private List<String> normalizeTenantIds(List<String> tenantIds) {
        Set<String> uniqueTenantIds = new LinkedHashSet<>(tenantIds);
        if (uniqueTenantIds.size() != tenantIds.size()) {
            throw new BadRequestException("Tenant ids must be unique");
        }

        return List.copyOf(uniqueTenantIds);
    }

    /**
     * Xác thực ngày bắt đầu phải trước ngày kết thúc.
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (!startDate.isBefore(endDate)) {
            throw new BadRequestException("Start date must be before end date");
        }
    }

    /**
     * Đảm bảo phòng chưa có bất kỳ hợp đồng nào đang ACTIVE (đang thuê).
     */
    private void ensureRoomHasNoActiveContract(String roomId, String ownerId) {
        contractRepository.findByRoomIdAndOwnerIdAndStatus(roomId, ownerId, ContractStatus.ACTIVE)
                .ifPresent(contract -> {
                    throw new BadRequestException("Room already has an active contract");
                });
    }

    private void ensureTenantsHaveNoActiveContract(List<String> tenantIds, String ownerId) {
        boolean hasActiveContract = contractRepository.existsByOwnerIdAndStatusAndTenantIdsIn(ownerId, ContractStatus.ACTIVE, tenantIds);
        if (hasActiveContract) {
            throw new BadRequestException("Tenant already has an active contract");
        }
    }

    private void validateRoomForNewContract(Room room) {
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new BadRequestException("Room must be available before creating a contract");
        }
    }

    private void validateRoomCapacity(Room room, List<String> tenantIds) {
        if (room.getMaxTenants() != null && tenantIds.size() > room.getMaxTenants()) {
            throw new BadRequestException("Tenant count exceeds room capacity");
        }
    }

    /**
     * Xác định trạng thái của phòng sau khi chấm dứt hợp đồng.
     * Trạng thái mặc định là AVAILABLE (trống). Hoặc MAINTENANCE (sửa chữa).
     */
    private RoomStatus resolveTerminateRoomStatus(TerminateContractRequest request) {
        RoomStatus roomStatus = request == null || request.getRoomStatus() == null
                ? RoomStatus.AVAILABLE
                : request.getRoomStatus();

        if (roomStatus != RoomStatus.AVAILABLE && roomStatus != RoomStatus.MAINTENANCE) {
            throw new BadRequestException("Room status after termination must be AVAILABLE or MAINTENANCE");
        }

        return roomStatus;
    }
}
