package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.common.util.SoftDeleteHelper;
import com.example.boardinghouse.domain.entity.Room;
import com.example.boardinghouse.domain.enums.RoomStatus;
import com.example.boardinghouse.dto.room.CreateRoomRequest;
import com.example.boardinghouse.dto.room.UpdateRoomRequest;
import com.example.boardinghouse.repository.PropertyRepository;
import com.example.boardinghouse.repository.RoomRepository;
import com.example.boardinghouse.security.CurrentUserService;
import com.example.boardinghouse.realtime.RealtimeEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final PropertyRepository propertyRepository;
    private final MongoTemplate mongoTemplate;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    /**
     * Lấy danh sách các phòng thuộc một tòa nhà/khu trọ cụ thể.
     * Kiểm tra xem tòa nhà có tồn tại hay không trước khi lấy dữ liệu.
     *
     * @param propertyId ID của tòa nhà/khu trọ
     * @return Danh sách các phòng
     */
    public List<Room> getRoomsByPropertyId(String propertyId) {
        String ownerId = currentUserService.getOwnerId();
        ensurePropertyExists(propertyId, ownerId);
        return roomRepository.findByPropertyIdAndOwnerId(propertyId, ownerId);
    }

    /**
     * Tạo mới một phòng trong một tòa nhà/khu trọ.
     * Kiểm tra tòa nhà phải tồn tại và số phòng chưa được sử dụng trong tòa nhà đó.
     * Mặc định trạng thái phòng là AVAILABLE (Trống) nếu không truyền vào.
     *
     * @param propertyId ID của tòa nhà
     * @param request Dữ liệu tạo phòng
     * @return Phòng vừa được tạo
     */
    public Room createRoom(String propertyId, CreateRoomRequest request) {
        String ownerId = currentUserService.getOwnerId();
        ensurePropertyExists(propertyId, ownerId);
        ensureRoomNumberAvailable(propertyId, ownerId, request.getRoomNumber(), null);

        Room room = Room.builder()
                .ownerId(ownerId)
                .propertyId(propertyId)
                .roomNumber(request.getRoomNumber())
                .floor(request.getFloor())
                .area(request.getArea())
                .baseRent(request.getBaseRent())
                .deposit(request.getDeposit())
                .maxTenants(request.getMaxTenants())
                .status(request.getStatus() == null ? RoomStatus.AVAILABLE : request.getStatus())
                .note(request.getNote())
                .build();

        Room saved = roomRepository.save(room);
        auditService.log("CREATE", "ROOM", saved.getId(), null, saved);
        realtimeEventPublisher.publishGlobalUpdate();
        return saved;
    }

    /**
     * Lấy thông tin chi tiết của một phòng theo ID.
     * Nếu không tìm thấy sẽ ném ra ngoại lệ.
     *
     * @param id ID của phòng
     * @return Thông tin phòng
     */
    public Room getRoomById(String id) {
        return roomRepository.findByIdAndOwnerId(id, currentUserService.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
    }

    /**
     * Cập nhật thông tin phòng.
     * Kiểm tra xem số phòng mới cập nhật có bị trùng với phòng khác trong cùng tòa nhà không.
     *
     * @param id ID của phòng cần cập nhật
     * @param request Dữ liệu cập nhật
     * @return Phòng sau khi đã cập nhật
     */
    public Room updateRoom(String id, UpdateRoomRequest request) {
        Room room = getRoomById(id);
        ensureRoomNumberAvailable(room.getPropertyId(), room.getOwnerId(), request.getRoomNumber(), id);

        room.setRoomNumber(request.getRoomNumber());
        room.setFloor(request.getFloor());
        room.setArea(request.getArea());
        room.setBaseRent(request.getBaseRent());
        room.setDeposit(request.getDeposit());
        room.setMaxTenants(request.getMaxTenants());
        RoomStatus requestedStatus = request.getStatus() == null ? room.getStatus() : request.getStatus();
        validateStatusChange(room, requestedStatus);
        room.setStatus(requestedStatus);
        room.setNote(request.getNote());

        Room saved = roomRepository.save(room);
        auditService.log("UPDATE", "ROOM", saved.getId(), null, saved);
        realtimeEventPublisher.publishGlobalUpdate();
        return saved;
    }

    /**
     * Cập nhật nhanh trạng thái của phòng (ví dụ: đang trống, đang sửa chữa, đã thuê).
     *
     * @param id ID của phòng
     * @param status Trạng thái mới
     * @return Phòng sau khi cập nhật trạng thái
     */
    public Room updateRoomStatus(String id, RoomStatus status) {
        Room room = getRoomById(id);
        validateStatusChange(room, status);
        room.setStatus(status);
        Room saved = roomRepository.save(room);
        auditService.log("STATUS_CHANGE", "ROOM", saved.getId(), null, saved);
        realtimeEventPublisher.publishGlobalUpdate();
        return saved;
    }

    /**
     * Xóa một phòng khỏi hệ thống.
     * Kiểm tra xem phòng có đang có hợp đồng thuê nào đang hoạt động (ACTIVE) không.
     * Nếu có, không cho phép xóa.
     *
     * @param id ID của phòng cần xóa
     */
    public void deleteRoom(String id) {
        Room room = getRoomById(id);
        long activeContractCount = mongoTemplate.count(
                Query.query(Criteria.where("roomId").is(id)
                        .and("ownerId").is(room.getOwnerId())
                        .and("status").is("ACTIVE")
                        .and("deleted").ne(true)),
                "contracts"
        );

        if (activeContractCount > 0) {
            throw new BadRequestException("Cannot delete room because it has an active contract");
        }

        SoftDeleteHelper.markDeleted(room, room.getOwnerId());
        roomRepository.save(room);
        auditService.log("SOFT_DELETE", "ROOM", room.getId(), null, room);
        realtimeEventPublisher.publishGlobalUpdate();
    }

    /**
     * Hàm hỗ trợ kiểm tra xem tòa nhà/khu trọ có tồn tại không.
     *
     * @param propertyId ID tòa nhà
     */
    private void ensurePropertyExists(String propertyId, String ownerId) {
        if (propertyRepository.findByIdAndOwnerId(propertyId, ownerId).isEmpty()) {
            throw new ResourceNotFoundException("Property not found with id: " + propertyId);
        }
    }

    /**
     * Hàm hỗ trợ kiểm tra xem số phòng đã tồn tại trong tòa nhà hay chưa.
     * Dùng để tránh việc tạo 2 phòng cùng số trong 1 tòa nhà.
     *
     * @param propertyId ID tòa nhà
     * @param roomNumber Số phòng cần kiểm tra
     * @param currentRoomId ID phòng hiện tại (dùng khi cập nhật để bỏ qua chính nó)
     */
    private void ensureRoomNumberAvailable(String propertyId, String ownerId, String roomNumber, String currentRoomId) {
        roomRepository.findByPropertyIdAndRoomNumberAndOwnerId(propertyId, roomNumber, ownerId)
                .filter(room -> currentRoomId == null || !room.getId().equals(currentRoomId))
                .ifPresent(room -> {
                    throw new BadRequestException("Room number already exists in this property");
                });
    }

    private void validateStatusChange(Room room, RoomStatus status) {
        long activeContractCount = mongoTemplate.count(
                Query.query(Criteria.where("roomId").is(room.getId())
                        .and("ownerId").is(room.getOwnerId())
                        .and("status").is("ACTIVE")
                        .and("deleted").ne(true)),
                "contracts"
        );

        if (activeContractCount > 0 && status != RoomStatus.OCCUPIED) {
            throw new BadRequestException("Cannot change room status while it has an active contract");
        }
    }
}
