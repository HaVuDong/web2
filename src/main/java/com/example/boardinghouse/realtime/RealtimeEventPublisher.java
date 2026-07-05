package com.example.boardinghouse.realtime;

import com.example.boardinghouse.domain.entity.Payment;
import com.example.boardinghouse.domain.enums.InvoiceStatus;
import com.example.boardinghouse.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Thành phần chịu trách nhiệm tạo và phát (publish) các sự kiện realtime.
 * Hoạt động như một cầu nối giữa các Service (như PaymentService) và WebSocket Handler.
 */
@Component
@RequiredArgsConstructor
public class RealtimeEventPublisher {

    public static final String PAYMENT_UPDATED = "PAYMENT_UPDATED";
    public static final String GLOBAL_UPDATE = "GLOBAL_UPDATE";

    private final RealtimeWebSocketHandler realtimeWebSocketHandler;
    private final CurrentUserService currentUserService;

    /**
     * Gửi thông báo cho tất cả client đang kết nối WebSocket về việc một khoản thanh toán vừa được cập nhật.
     */
    public void publishPaymentUpdated(Payment payment, InvoiceStatus invoiceStatus) {
        PaymentUpdatedData data = new PaymentUpdatedData(
                payment.getId(),
                payment.getInvoiceId(),
                payment.getStatus(),
                invoiceStatus,
                payment.getPaidAt()
        );
        String ownerId = currentUserService.getOwnerId();
        realtimeWebSocketHandler.broadcastToOwner(RealtimeEvent.of(PAYMENT_UPDATED, data), ownerId);
    }

    /**
     * Gửi thông báo cho tất cả client để đồng bộ hóa dữ liệu (invalidate all queries).
     */
    public void publishGlobalUpdate() {
        String ownerId = currentUserService.getOwnerId();
        realtimeWebSocketHandler.broadcastToOwner(RealtimeEvent.of(GLOBAL_UPDATE, null), ownerId);
    }
}
