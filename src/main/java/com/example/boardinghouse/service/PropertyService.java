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

    public List<Property> getAllProperties() {
        return propertyRepository.findAll();
    }

    public Property getPropertyById(String id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));
    }

    public Property createProperty(CreatePropertyRequest request, String createdBy) {
        Property property = Property.builder()
                .name(request.getName())
                .address(request.getAddress())
                .description(request.getDescription())
                .createdBy(createdBy)
                .build();

        return propertyRepository.save(property);
    }

    public Property updateProperty(String id, UpdatePropertyRequest request) {
        Property property = getPropertyById(id);
        property.setName(request.getName());
        property.setAddress(request.getAddress());
        property.setDescription(request.getDescription());

        return propertyRepository.save(property);
    }

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
