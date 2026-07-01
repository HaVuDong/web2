package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.domain.entity.Tenant;
import com.example.boardinghouse.domain.enums.TenantStatus;
import com.example.boardinghouse.dto.tenant.CreateTenantRequest;
import com.example.boardinghouse.dto.tenant.UpdateTenantRequest;
import com.example.boardinghouse.repository.RoomRepository;
import com.example.boardinghouse.repository.TenantRepository;
import com.example.boardinghouse.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final RoomRepository roomRepository;
    private final MongoTemplate mongoTemplate;
    private final CurrentUserService currentUserService;

    /**
     * Lấy danh sách khách thuê trọ dựa trên từ khóa tìm kiếm (tên, số điện thoại) và trạng thái.
     * Nếu không có từ khóa và trạng thái, sẽ trả về toàn bộ danh sách.
     *
     * @param keyword Từ khóa tìm kiếm (tên, số điện thoại)
     * @param status Trạng thái của khách thuê (ví dụ: ACTIVE, LEFT)
     * @return Danh sách khách thuê phù hợp với điều kiện
     */
    public List<Tenant> getTenants(String keyword, TenantStatus status) {
        String ownerId = currentUserService.getOwnerId();
        boolean hasKeyword = StringUtils.hasText(keyword);
        if (!hasKeyword && status == null) {
            return tenantRepository.findByOwnerId(ownerId);
        }

        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("ownerId").is(ownerId));
        criteria.add(Criteria.where("deleted").ne(true));
        if (hasKeyword) {
            String escapedKeyword = Pattern.quote(keyword.trim());
            criteria.add(new Criteria().orOperator(
                    Criteria.where("fullName").regex(escapedKeyword, "i"),
                    Criteria.where("phone").regex(escapedKeyword, "i")
            ));
        }

        if (status != null) {
            criteria.add(Criteria.where("status").is(status));
        }

        Query query = new Query();
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }

        return mongoTemplate.find(query, Tenant.class);
    }

    /**
     * Tạo mới một khách thuê trọ.
     * Mặc định nếu không truyền trạng thái thì sẽ gán là ACTIVE.
     * Kiểm tra phòng hiện tại có tồn tại hay không trước khi lưu.
     *
     * @param request Dữ liệu đầu vào để tạo khách thuê
     * @return Khách thuê mới vừa được lưu vào CSDL
     */
    public Tenant createTenant(CreateTenantRequest request) {
        String ownerId = currentUserService.getOwnerId();
        TenantStatus status = request.getStatus() == null ? TenantStatus.ACTIVE : request.getStatus();
        String currentRoomId = resolveCurrentRoomId(request.getCurrentRoomId(), status, ownerId);

        Tenant tenant = Tenant.builder()
                .ownerId(ownerId)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .identityNumber(request.getIdentityNumber())
                .dateOfBirth(request.getDateOfBirth())
                .permanentAddress(request.getPermanentAddress())
                .currentRoomId(currentRoomId)
                .status(status)
                .note(request.getNote())
                .build();

        return tenantRepository.save(tenant);
    }

    /**
     * Lấy thông tin khách thuê theo ID.
     * Ném ra ngoại lệ ResourceNotFoundException nếu không tìm thấy.
     *
     * @param id ID của khách thuê
     * @return Thông tin khách thuê
     */
    public Tenant getTenantById(String id) {
        return tenantRepository.findByIdAndOwnerId(id, currentUserService.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + id));
    }

    /**
     * Cập nhật thông tin của khách thuê hiện tại.
     * Lấy thông tin cũ ra, cập nhật các trường mới và kiểm tra tính hợp lệ của phòng nếu có thay đổi.
     *
     * @param id ID của khách thuê cần cập nhật
     * @param request Dữ liệu cập nhật
     * @return Khách thuê đã được cập nhật
     */
    public Tenant updateTenant(String id, UpdateTenantRequest request) {
        Tenant tenant = getTenantById(id);
        String ownerId = tenant.getOwnerId();
        TenantStatus status = request.getStatus() == null ? tenant.getStatus() : request.getStatus();
        String currentRoomId;
        // Cần kiểm tra xem khách thuê có hợp đồng đang hoạt động không nếu trạng thái đổi thành LEFT
        if (status == TenantStatus.LEFT && tenant.getStatus() != TenantStatus.LEFT) {
            ensureTenantHasNoActiveContract(id, tenant.getOwnerId());
            currentRoomId = null; // Tự động xóa phòng hiện tại khi rời đi
        } else {
            currentRoomId = resolveCurrentRoomId(request.getCurrentRoomId(), status, ownerId);
        }

        tenant.setFullName(request.getFullName());
        tenant.setPhone(request.getPhone());
        tenant.setEmail(request.getEmail());
        tenant.setIdentityNumber(request.getIdentityNumber());
        tenant.setDateOfBirth(request.getDateOfBirth());
        tenant.setPermanentAddress(request.getPermanentAddress());
        tenant.setCurrentRoomId(currentRoomId);
        tenant.setStatus(status);
        tenant.setNote(request.getNote());

        return tenantRepository.save(tenant);
    }

    /**
     * Đánh dấu khách thuê đã rời đi (LEFT).
     * Kiểm tra xem khách thuê này có hợp đồng nào đang ACTIVE hay không.
     * Nếu có, không cho phép đánh dấu đã rời đi và ném ra ngoại lệ.
     * Đồng thời xóa thông tin phòng hiện tại của khách thuê.
     *
     * @param id ID của khách thuê
     */
    public void markTenantLeft(String id) {
        Tenant tenant = getTenantById(id);
        
        ensureTenantHasNoActiveContract(id, tenant.getOwnerId());

        tenant.setStatus(TenantStatus.LEFT);
        tenant.setCurrentRoomId(null);
        tenantRepository.save(tenant);
    }

    /**
     * Lấy danh sách khách thuê đang ở trong một phòng cụ thể.
     * Kiểm tra phòng có tồn tại hay không trước khi truy vấn.
     *
     * @param roomId ID của phòng
     * @return Danh sách khách thuê trong phòng
     */
    public List<Tenant> getTenantsByRoomId(String roomId) {
        String ownerId = currentUserService.getOwnerId();
        if (roomRepository.findByIdAndOwnerId(roomId, ownerId).isEmpty()) {
            throw new ResourceNotFoundException("Room not found with id: " + roomId);
        }

        return tenantRepository.findByCurrentRoomIdAndOwnerId(roomId, ownerId);
    }

    /**
     * Xử lý và xác thực ID phòng hiện tại của khách thuê.
     * Nếu khách thuê đã rời đi (LEFT), phòng hiện tại sẽ là null.
     * Nếu có ID phòng, kiểm tra xem phòng đó có tồn tại trong CSDL hay không.
     *
     * @param currentRoomId ID phòng đầu vào
     * @param status Trạng thái khách thuê
     * @return ID phòng hợp lệ hoặc null
     */
    private String resolveCurrentRoomId(String currentRoomId, TenantStatus status, String ownerId) {
        if (status == TenantStatus.LEFT) {
            return null;
        }

        if (StringUtils.hasText(currentRoomId) && roomRepository.findByIdAndOwnerId(currentRoomId, ownerId).isEmpty()) {
            throw new ResourceNotFoundException("Room not found with id: " + currentRoomId);
        }

        return StringUtils.hasText(currentRoomId) ? currentRoomId : null;
    }

    /**
     * Kiểm tra xem khách thuê có hợp đồng nào đang ACTIVE hay không.
     * Dùng khi đổi trạng thái khách thuê sang LEFT (đã rời đi).
     */
    private void ensureTenantHasNoActiveContract(String tenantId, String ownerId) {
        long activeContractCount = mongoTemplate.count(
                Query.query(Criteria.where("tenantIds").is(tenantId)
                        .and("ownerId").is(ownerId)
                        .and("status").is("ACTIVE")
                        .and("deleted").ne(true)),
                "contracts"
        );

        if (activeContractCount > 0) {
            throw new BadRequestException("Cannot mark tenant as left because tenant has an active contract");
        }
    }
}
