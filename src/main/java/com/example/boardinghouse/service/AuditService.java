package com.example.boardinghouse.service;

import com.example.boardinghouse.domain.entity.AuditLog;
import com.example.boardinghouse.repository.AuditLogRepository;
import com.example.boardinghouse.security.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    /** Fields tuyệt đối không được ghi vào audit log */
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "passwordHash", "token", "jwt", "secret",
            "apiKey", "clientId", "checksumKey", "signature",
            "rawWebhookData"
    );

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    /**
     * Ghi log một thao tác thành công.
     * Fire-and-forget: exception trong audit log không ảnh hưởng đến nghiệp vụ chính.
     *
     * @param action     Loại thao tác: CREATE, UPDATE, DELETE, SOFT_DELETE, LOGIN, PAY, CANCEL, TERMINATE, RENEW, WEBHOOK, STATUS_CHANGE
     * @param entityType Loại entity: PROPERTY, ROOM, TENANT, CONTRACT, INVOICE, METER_READING, MAINTENANCE, PAYMENT
     * @param entityId   ID entity bị tác động
     * @param before     Snapshot trước thay đổi (null cho CREATE)
     * @param after      Snapshot sau thay đổi (null cho hard DELETE)
     */
    public void log(String action, String entityType, String entityId, Object before, Object after) {
        try {
            String ownerId = currentUserService.getOwnerId();
            Map<String, Object> beforeMap = toSanitizedMap(before);
            Map<String, Object> afterMap = toSanitizedMap(after);
            List<String> changedFields = detectChangedFields(beforeMap, afterMap);

            AuditLog auditLog = AuditLog.builder()
                    .ownerId(ownerId)
                    .actorId(ownerId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .before(beforeMap)
                    .after(afterMap)
                    .changedFields(changedFields)
                    .success(true)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.warn("Failed to write audit log: action={}, entityType={}, entityId={}", action, entityType, entityId, ex);
        }
    }

    /**
     * Ghi log cho webhook (không cần authentication context).
     */
    public void logWebhook(String entityType, String entityId, Object before, Object after) {
        try {
            Map<String, Object> beforeMap = toSanitizedMap(before);
            Map<String, Object> afterMap = toSanitizedMap(after);

            AuditLog auditLog = AuditLog.builder()
                    .ownerId(afterMap != null ? (String) afterMap.get("ownerId") : null)
                    .actorId("SYSTEM")
                    .action("WEBHOOK")
                    .entityType(entityType)
                    .entityId(entityId)
                    .before(beforeMap)
                    .after(afterMap)
                    .success(true)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.warn("Failed to write webhook audit log: entityType={}, entityId={}", entityType, entityId, ex);
        }
    }

    /**
     * Truy vấn audit log theo owner với phân trang.
     */
    public Page<AuditLog> query(String entityType, String entityId, String action, int page, int size) {
        String ownerId = currentUserService.getOwnerId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (StringUtils.hasText(entityId)) {
            return auditLogRepository.findByOwnerIdAndEntityId(ownerId, entityId, pageable);
        }

        boolean hasEntityType = StringUtils.hasText(entityType);
        boolean hasAction = StringUtils.hasText(action);

        if (hasEntityType && hasAction) {
            return auditLogRepository.findByOwnerIdAndEntityTypeAndAction(ownerId, entityType, action, pageable);
        }
        if (hasEntityType) {
            return auditLogRepository.findByOwnerIdAndEntityType(ownerId, entityType, pageable);
        }
        if (hasAction) {
            return auditLogRepository.findByOwnerIdAndAction(ownerId, action, pageable);
        }

        return auditLogRepository.findByOwnerId(ownerId, pageable);
    }

    /**
     * Chuyển đổi object thành Map và loại bỏ các field nhạy cảm.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> toSanitizedMap(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            Map<String, Object> map = objectMapper.convertValue(obj, LinkedHashMap.class);
            SENSITIVE_FIELDS.forEach(map::remove);
            return map;
        } catch (Exception ex) {
            log.warn("Failed to convert object to map for audit", ex);
            return null;
        }
    }

    /**
     * Phát hiện các field đã thay đổi giữa before và after.
     */
    private List<String> detectChangedFields(Map<String, Object> before, Map<String, Object> after) {
        if (before == null || after == null) {
            return null;
        }

        List<String> changed = new ArrayList<>();
        for (Map.Entry<String, Object> entry : after.entrySet()) {
            String key = entry.getKey();
            Object afterValue = entry.getValue();
            Object beforeValue = before.get(key);
            if (afterValue == null && beforeValue == null) {
                continue;
            }
            if (afterValue == null || !afterValue.equals(beforeValue)) {
                changed.add(key);
            }
        }

        return changed.isEmpty() ? null : changed;
    }
}
