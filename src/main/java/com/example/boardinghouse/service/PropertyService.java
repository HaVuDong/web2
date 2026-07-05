package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.common.util.SoftDeleteHelper;
import com.example.boardinghouse.domain.entity.Property;
import com.example.boardinghouse.dto.property.CreatePropertyRequest;
import com.example.boardinghouse.dto.property.UpdatePropertyRequest;
import com.example.boardinghouse.repository.PropertyRepository;
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
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final MongoTemplate mongoTemplate;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    /**
     * Lấy danh sách tất cả các tòa nhà/khu trọ trong hệ thống.
     *
     * @return Danh sách tòa nhà
     */
    public List<Property> getAllProperties() {
        return propertyRepository.findByOwnerId(currentUserService.getOwnerId());
    }

    /**
     * Lấy thông tin một tòa nhà theo ID. Ném ngoại lệ nếu không tìm thấy.
     */
    public Property getPropertyById(String id) {
        return propertyRepository.findByIdAndOwnerId(id, currentUserService.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));
    }

    /**
     * Tạo mới một tòa nhà/khu trọ.
     *
     * @param request Dữ liệu đầu vào
     * @param createdBy Người tạo (thường là email/ID của người dùng đang đăng nhập)
     * @return Tòa nhà vừa tạo
     */
    public Property createProperty(CreatePropertyRequest request, String createdBy) {
        String ownerId = currentUserService.getOwnerId();
        Property property = Property.builder()
                .ownerId(ownerId)
                .name(request.getName())
                .address(request.getAddress())
                .description(request.getDescription())
                .createdBy(createdBy)
                .build();

        Property saved = propertyRepository.save(property);
        auditService.log("CREATE", "PROPERTY", saved.getId(), null, saved);
        realtimeEventPublisher.publishGlobalUpdate();
        return saved;
    }

    /**
     * Cập nhật thông tin của tòa nhà/khu trọ.
     */
    public Property updateProperty(String id, UpdatePropertyRequest request) {
        Property property = getPropertyById(id);
        Property before = Property.builder()
                .id(property.getId()).ownerId(property.getOwnerId())
                .name(property.getName()).address(property.getAddress())
                .description(property.getDescription()).build();

        property.setName(request.getName());
        property.setAddress(request.getAddress());
        property.setDescription(request.getDescription());

        Property saved = propertyRepository.save(property);
        auditService.log("UPDATE", "PROPERTY", saved.getId(), before, saved);
        realtimeEventPublisher.publishGlobalUpdate();
        return saved;
    }

    /**
     * Xóa một tòa nhà khỏi hệ thống.
     * Nếu tòa nhà đang có phòng bên trong thì không cho phép xóa.
     */
    public void deleteProperty(String id) {
        Property property = getPropertyById(id);
        long roomCount = mongoTemplate.count(
                Query.query(Criteria.where("propertyId").is(id)
                        .and("ownerId").is(property.getOwnerId())
                        .and("deleted").ne(true)),
                "rooms"
        );

        if (roomCount > 0) {
            throw new BadRequestException("Cannot delete property because it still has rooms");
        }

        SoftDeleteHelper.markDeleted(property, currentUserService.getOwnerId());
        propertyRepository.save(property);
        auditService.log("SOFT_DELETE", "PROPERTY", property.getId(), null, property);
        realtimeEventPublisher.publishGlobalUpdate();
    }
}
