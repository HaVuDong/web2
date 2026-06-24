package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.domain.entity.Property;
import com.example.boardinghouse.dto.property.CreatePropertyRequest;
import com.example.boardinghouse.dto.property.UpdatePropertyRequest;
import com.example.boardinghouse.repository.PropertyRepository;
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

    /**
     * Lấy danh sách tất cả các tòa nhà/khu trọ trong hệ thống.
     *
     * @return Danh sách tòa nhà
     */
    public List<Property> getAllProperties() {
        return propertyRepository.findAll();
    }

    /**
     * Lấy thông tin một tòa nhà theo ID. Ném ngoại lệ nếu không tìm thấy.
     */
    public Property getPropertyById(String id) {
        return propertyRepository.findById(id)
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
        Property property = Property.builder()
                .name(request.getName())
                .address(request.getAddress())
                .description(request.getDescription())
                .createdBy(createdBy)
                .build();

        return propertyRepository.save(property);
    }

    /**
     * Cập nhật thông tin của tòa nhà/khu trọ.
     */
    public Property updateProperty(String id, UpdatePropertyRequest request) {
        Property property = getPropertyById(id);
        property.setName(request.getName());
        property.setAddress(request.getAddress());
        property.setDescription(request.getDescription());

        return propertyRepository.save(property);
    }

    /**
     * Xóa một tòa nhà khỏi hệ thống.
     * Nếu tòa nhà đang có phòng bên trong thì không cho phép xóa.
     */
    public void deleteProperty(String id) {
        Property property = getPropertyById(id);
        long roomCount = mongoTemplate.count(
                Query.query(Criteria.where("propertyId").is(id)),
                "rooms"
        );

        if (roomCount > 0) {
            throw new BadRequestException("Cannot delete property because it still has rooms");
        }

        propertyRepository.delete(property);
    }
}
