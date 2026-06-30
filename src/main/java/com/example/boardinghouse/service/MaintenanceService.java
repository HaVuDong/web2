package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.common.util.SoftDeleteHelper;
import com.example.boardinghouse.domain.entity.MaintenanceRequest;
import com.example.boardinghouse.domain.enums.MaintenanceStatus;
import com.example.boardinghouse.dto.maintenance.CreateMaintenanceRequest;
import com.example.boardinghouse.dto.maintenance.UpdateMaintenanceRequest;
import com.example.boardinghouse.repository.MaintenanceRepository;
import com.example.boardinghouse.repository.RoomRepository;
import com.example.boardinghouse.repository.TenantRepository;
import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.security.CurrentUserService;
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
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    /**
     * Lấy danh sách yêu cầu bảo trì. Nếu truyền vào trạng thái, sẽ lọc theo trạng thái đó.
     *
     * @param status Trạng thái yêu cầu bảo trì (ví dụ: PENDING, IN_PROGRESS, DONE)
     * @return Danh sách yêu cầu bảo trì
     */
    public List<MaintenanceRequest> getMaintenanceRequests(MaintenanceStatus status) {
        String ownerId = currentUserService.getOwnerId();
        return status == null
                ? maintenanceRepository.findByOwnerId(ownerId)
                : maintenanceRepository.findByOwnerIdAndStatus(ownerId, status);
    }

    /**
     * Tạo một yêu cầu bảo trì mới.
     * Kiểm tra phòng phải tồn tại, và khách thuê (nếu có truyền) phải hợp lệ.
     * Trạng thái mặc định khi mới tạo là PENDING.
     *
     * @param request Dữ liệu đầu vào để tạo yêu cầu
     * @return Yêu cầu bảo trì mới
     */
    public MaintenanceRequest createMaintenanceRequest(CreateMaintenanceRequest request) {
        String ownerId = currentUserService.getOwnerId();
        ensureRoomExists(request.getRoomId(), ownerId);
        ensureTenantExistsIfProvided(request.getTenantId(), ownerId);

        MaintenanceRequest maintenanceRequest = MaintenanceRequest.builder()
                .ownerId(ownerId)
                .roomId(request.getRoomId())
                .tenantId(request.getTenantId())
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .status(MaintenanceStatus.PENDING)
                .build();

        MaintenanceRequest saved = maintenanceRepository.save(maintenanceRequest);
        auditService.log("CREATE", "MAINTENANCE", saved.getId(), null, saved);
        return saved;
    }

    /**
     * Lấy thông tin yêu cầu bảo trì theo ID. Ném ngoại lệ nếu không tìm thấy.
     */
    public MaintenanceRequest getMaintenanceRequestById(String id) {
        return maintenanceRepository.findByIdAndOwnerId(id, currentUserService.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found with id: " + id));
    }

    /**
     * Cập nhật nội dung của yêu cầu bảo trì (người yêu cầu, tiêu đề, mô tả, mức độ ưu tiên).
     *
     * @param id ID của yêu cầu bảo trì
     * @param request Dữ liệu cập nhật
     * @return Yêu cầu bảo trì sau khi được cập nhật
     */
    public MaintenanceRequest updateMaintenanceRequest(String id, UpdateMaintenanceRequest request) {
        MaintenanceRequest maintenanceRequest = getMaintenanceRequestById(id);
        
        // Không cho phép cập nhật nếu yêu cầu đã hoàn thành hoặc đã bị hủy
        if (maintenanceRequest.getStatus() == MaintenanceStatus.DONE || maintenanceRequest.getStatus() == MaintenanceStatus.CANCELLED) {
            throw new BadRequestException("Cannot update a completed or cancelled maintenance request");
        }
        
        ensureTenantExistsIfProvided(request.getTenantId(), maintenanceRequest.getOwnerId());

        maintenanceRequest.setTenantId(request.getTenantId());
        maintenanceRequest.setTitle(request.getTitle());
        maintenanceRequest.setDescription(request.getDescription());
        maintenanceRequest.setPriority(request.getPriority());

        MaintenanceRequest saved = maintenanceRepository.save(maintenanceRequest);
        auditService.log("UPDATE", "MAINTENANCE", saved.getId(), null, saved);
        return saved;
    }

    /**
     * Cập nhật trạng thái của yêu cầu bảo trì.
     * Nếu chuyển sang trạng thái DONE, sẽ tự động lưu lại thời gian hoàn thành (completedAt).
     *
     * @param id ID của yêu cầu
     * @param status Trạng thái mới
     * @return Yêu cầu sau khi cập nhật
     */
    public MaintenanceRequest updateMaintenanceStatus(String id, MaintenanceStatus status) {
        MaintenanceRequest maintenanceRequest = getMaintenanceRequestById(id);

        if (maintenanceRequest.getStatus() == MaintenanceStatus.DONE) {
            throw new BadRequestException("Completed maintenance request cannot change status");
        }
        if (maintenanceRequest.getStatus() == MaintenanceStatus.CANCELLED) {
            throw new BadRequestException("Cancelled maintenance request cannot change status");
        }

        maintenanceRequest.setStatus(status);
        maintenanceRequest.setCompletedAt(status == MaintenanceStatus.DONE ? LocalDateTime.now() : null);
        MaintenanceRequest saved = maintenanceRepository.save(maintenanceRequest);
        auditService.log("STATUS_CHANGE", "MAINTENANCE", saved.getId(), null, saved);
        return saved;
    }

    /**
     * Xóa yêu cầu bảo trì khỏi hệ thống.
     */
    public void deleteMaintenanceRequest(String id) {
        MaintenanceRequest maintenanceRequest = getMaintenanceRequestById(id);
        if (maintenanceRequest.getStatus() == MaintenanceStatus.DONE) {
            throw new BadRequestException("Completed maintenance request cannot be deleted");
        }
        SoftDeleteHelper.markDeleted(maintenanceRequest, currentUserService.getOwnerId());
        maintenanceRepository.save(maintenanceRequest);
        auditService.log("SOFT_DELETE", "MAINTENANCE", maintenanceRequest.getId(), null, maintenanceRequest);
    }

    /**
     * Đảm bảo phòng phải tồn tại trong cơ sở dữ liệu.
     */
    private void ensureRoomExists(String roomId, String ownerId) {
        if (roomRepository.findByIdAndOwnerId(roomId, ownerId).isEmpty()) {
            throw new ResourceNotFoundException("Room not found with id: " + roomId);
        }
    }

    /**
     * Nếu có truyền ID khách thuê, đảm bảo khách thuê đó phải tồn tại trong CSDL.
     */
    private void ensureTenantExistsIfProvided(String tenantId, String ownerId) {
        if (tenantId != null && !tenantId.isBlank() && tenantRepository.findByIdAndOwnerId(tenantId, ownerId).isEmpty()) {
            throw new ResourceNotFoundException("Tenant not found with id: " + tenantId);
        }
    }
}
