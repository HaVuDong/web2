package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.domain.entity.Tenant;
import com.example.boardinghouse.domain.enums.TenantStatus;
import com.example.boardinghouse.dto.tenant.CreateTenantRequest;
import com.example.boardinghouse.dto.tenant.UpdateTenantRequest;
import com.example.boardinghouse.repository.RoomRepository;
import com.example.boardinghouse.repository.TenantRepository;
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

    public List<Tenant> getTenants(String keyword, TenantStatus status) {
        boolean hasKeyword = StringUtils.hasText(keyword);
        if (!hasKeyword && status == null) {
            return tenantRepository.findAll();
        }

        List<Criteria> criteria = new ArrayList<>();
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

    public Tenant createTenant(CreateTenantRequest request) {
        TenantStatus status = request.getStatus() == null ? TenantStatus.ACTIVE : request.getStatus();
        String currentRoomId = resolveCurrentRoomId(request.getCurrentRoomId(), status);

        Tenant tenant = Tenant.builder()
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

    public Tenant getTenantById(String id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + id));
    }

    public Tenant updateTenant(String id, UpdateTenantRequest request) {
        Tenant tenant = getTenantById(id);
        TenantStatus status = request.getStatus() == null ? tenant.getStatus() : request.getStatus();
        String currentRoomId = resolveCurrentRoomId(request.getCurrentRoomId(), status);

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

    public void markTenantLeft(String id) {
        Tenant tenant = getTenantById(id);
        long activeContractCount = mongoTemplate.count(
                Query.query(Criteria.where("tenantIds").is(id).and("status").is("ACTIVE")),
                "contracts"
        );

        if (activeContractCount > 0) {
            throw new BadRequestException("Cannot mark tenant as left because tenant has an active contract");
        }

        tenant.setStatus(TenantStatus.LEFT);
        tenant.setCurrentRoomId(null);
        tenantRepository.save(tenant);
    }

    public List<Tenant> getTenantsByRoomId(String roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new ResourceNotFoundException("Room not found with id: " + roomId);
        }

        return tenantRepository.findByCurrentRoomId(roomId);
    }

    private String resolveCurrentRoomId(String currentRoomId, TenantStatus status) {
        if (status == TenantStatus.LEFT) {
            return null;
        }

        if (StringUtils.hasText(currentRoomId) && !roomRepository.existsById(currentRoomId)) {
            throw new ResourceNotFoundException("Room not found with id: " + currentRoomId);
        }

        return StringUtils.hasText(currentRoomId) ? currentRoomId : null;
    }
}
