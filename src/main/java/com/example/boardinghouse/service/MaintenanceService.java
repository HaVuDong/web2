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

    /**
     * Lấy danh sách yêu cầu bảo trì. Nếu truyền vào trạng thái, sẽ lọc theo trạng thái đó.
     *
     * @param status Trạng thái yêu cầu bảo trì (ví dụ: PENDING, IN_PROGRESS, DONE)
     * @return Danh sách yêu cầu bảo trì
     */
    public List<MaintenanceRequest> getMaintenanceRequests(MaintenanceStatus status) {
        return status == null ? maintenanceRepository.findAll() : maintenanceRepository.findByStatus(status);
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

    /**
     * Lấy thông tin yêu cầu bảo trì theo ID. Ném ngoại lệ nếu không tìm thấy.
     */
    public MaintenanceRequest getMaintenanceRequestById(String id) {
        return maintenanceRepository.findById(id)
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
        ensureTenantExistsIfProvided(request.getTenantId());

        maintenanceRequest.setTenantId(request.getTenantId());
        maintenanceRequest.setTitle(request.getTitle());
        maintenanceRequest.setDescription(request.getDescription());
        maintenanceRequest.setPriority(request.getPriority());

        return maintenanceRepository.save(maintenanceRequest);
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
        maintenanceRequest.setStatus(status);
        maintenanceRequest.setCompletedAt(status == MaintenanceStatus.DONE ? LocalDateTime.now() : null);
        return maintenanceRepository.save(maintenanceRequest);
    }

    /**
     * Xóa yêu cầu bảo trì khỏi hệ thống.
     */
    public void deleteMaintenanceRequest(String id) {
        MaintenanceRequest maintenanceRequest = getMaintenanceRequestById(id);
        maintenanceRepository.delete(maintenanceRequest);
    }

    /**
     * Đảm bảo phòng phải tồn tại trong cơ sở dữ liệu.
     */
    private void ensureRoomExists(String roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new ResourceNotFoundException("Room not found with id: " + roomId);
        }
    }

    /**
     * Nếu có truyền ID khách thuê, đảm bảo khách thuê đó phải tồn tại trong CSDL.
     */
    private void ensureTenantExistsIfProvided(String tenantId) {
        if (tenantId != null && !tenantId.isBlank() && !tenantRepository.existsById(tenantId)) {
            throw new ResourceNotFoundException("Tenant not found with id: " + tenantId);
        }
    }
}
