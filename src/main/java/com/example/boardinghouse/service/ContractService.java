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
import com.example.boardinghouse.dto.contract.TerminateContractRequest;
import com.example.boardinghouse.dto.contract.UpdateContractRequest;
import com.example.boardinghouse.repository.ContractRepository;
import com.example.boardinghouse.repository.RoomRepository;
import com.example.boardinghouse.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;

    /**
     * Lấy danh sách toàn bộ hợp đồng thuê phòng.
     *
     * @return Danh sách các hợp đồng
     */
    public List<Contract> getAllContracts() {
        return contractRepository.findAll();
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
        Room room = getRoom(request.getRoomId());
        List<String> tenantIds = normalizeTenantIds(request.getTenantIds());
        List<Tenant> tenants = getTenants(tenantIds);
        validateDateRange(request.getStartDate(), request.getEndDate());
        ensureRoomHasNoActiveContract(room.getId());

        Contract contract = Contract.builder()
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

        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);

        tenants.forEach(tenant -> {
            tenant.setCurrentRoomId(room.getId());
            tenant.setStatus(TenantStatus.ACTIVE);
        });
        tenantRepository.saveAll(tenants);

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
        return contractRepository.findById(id)
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

        return contractRepository.save(contract);
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

        roomRepository.findById(contract.getRoomId()).ifPresent(room -> {
            room.setStatus(roomStatus);
            roomRepository.save(room);
        });

        List<Tenant> tenants = tenantRepository.findAllById(contract.getTenantIds());
        tenants.forEach(tenant -> {
            tenant.setCurrentRoomId(null);
            tenant.setStatus(TenantStatus.LEFT);
        });
        tenantRepository.saveAll(tenants);

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

        return contractRepository.save(contract);
    }

    /**
     * Lấy thông tin phòng dựa theo ID, ném ngoại lệ nếu không có.
     */
    private Room getRoom(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));
    }

    /**
     * Lấy danh sách khách thuê theo ID, đảm bảo tất cả ID đều hợp lệ.
     */
    private List<Tenant> getTenants(List<String> tenantIds) {
        List<Tenant> tenants = tenantRepository.findAllById(tenantIds);
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
    private void ensureRoomHasNoActiveContract(String roomId) {
        contractRepository.findByRoomIdAndStatus(roomId, ContractStatus.ACTIVE)
                .ifPresent(contract -> {
                    throw new BadRequestException("Room already has an active contract");
                });
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
