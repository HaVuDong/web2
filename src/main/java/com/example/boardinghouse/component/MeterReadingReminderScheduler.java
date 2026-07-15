package com.example.boardinghouse.component;

import com.example.boardinghouse.domain.entity.Tenant;
import com.example.boardinghouse.domain.enums.TenantStatus;
import com.example.boardinghouse.repository.TenantRepository;
import com.example.boardinghouse.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled job gửi email nhắc khách thuê nhập chỉ số điện nước mỗi đầu tháng.
 * Chạy vào lúc 8h sáng ngày 1 hàng tháng.
 * Chỉ gửi cho tenant có status ACTIVE, có phòng hiện tại, và có email.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MeterReadingReminderScheduler {

    private final MongoTemplate mongoTemplate;
    private final EmailService emailService;

    /**
     * Gửi email nhắc nhở nhập chỉ số điện nước vào 8h sáng ngày 1 hàng tháng.
     * Cron: giây phút giờ ngày tháng thứ
     * "0 0 8 1 * *" = 08:00:00 ngày 1 mỗi tháng
     */
    @Scheduled(cron = "0 0 8 1 * *")
    public void sendMonthlyMeterReadingReminders() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();

        log.info("Starting monthly meter reading reminder for {}/{}", month, year);

        // Query tất cả tenant ACTIVE, có phòng, có email, chưa bị xóa
        Query query = Query.query(
                Criteria.where("status").is(TenantStatus.ACTIVE.name())
                        .and("currentRoomId").ne(null)
                        .and("email").ne(null)
                        .and("deleted").ne(true)
        );

        List<Tenant> tenants = mongoTemplate.find(query, Tenant.class);
        log.info("Found {} tenants to send meter reading reminders", tenants.size());

        int sent = 0;
        int failed = 0;
        for (Tenant tenant : tenants) {
            if (tenant.getEmail() == null || tenant.getEmail().isBlank()) {
                continue;
            }
            try {
                emailService.sendMeterReadingReminderEmail(
                        tenant.getEmail(),
                        tenant.getFullName(),
                        month,
                        year
                );
                sent++;
            } catch (Exception e) {
                failed++;
                log.error("Failed to send reminder to tenant {} ({}): {}",
                        tenant.getFullName(), tenant.getEmail(), e.getMessage());
            }
        }

        log.info("Meter reading reminder completed: {} sent, {} failed", sent, failed);
    }
}
