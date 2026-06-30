package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.common.util.OrderCodeGenerator;
import com.example.boardinghouse.config.PayosProperties;
import com.example.boardinghouse.domain.entity.Invoice;
import com.example.boardinghouse.domain.entity.Payment;
import com.example.boardinghouse.domain.enums.InvoiceStatus;
import com.example.boardinghouse.domain.enums.PaymentMethod;
import com.example.boardinghouse.domain.enums.PaymentProvider;
import com.example.boardinghouse.domain.enums.PaymentStatus;
import com.example.boardinghouse.dto.payment.ManualPaymentRequest;
import com.example.boardinghouse.dto.payment.PaymentLinkResponse;
import com.example.boardinghouse.dto.payment.PayosCreatePaymentLinkRequest;
import com.example.boardinghouse.dto.payment.PayosCreatePaymentLinkResponse;
import com.example.boardinghouse.dto.payment.PayosWebhookRequest;
import com.example.boardinghouse.repository.InvoiceRepository;
import com.example.boardinghouse.repository.PaymentRepository;
import com.example.boardinghouse.realtime.RealtimeEventPublisher;
import com.example.boardinghouse.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String PAYOS_SUCCESS_CODE = "00";
    private static final int MAX_ORDER_CODE_ATTEMPTS = 5;

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PayosGateway payosGateway;
    private final PayosSignatureService payosSignatureService;
    private final PayosProperties payosProperties;
    private final OrderCodeGenerator orderCodeGenerator;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    /**
     * Tạo một link thanh toán (QR code/Checkout URL) cho một hóa đơn.
     * Nếu hóa đơn đã được thanh toán hoặc đã hủy, sẽ ném ngoại lệ.
     * Nếu đã có một yêu cầu thanh toán đang PENDING trước đó, sẽ trả về lại link cũ thay vì tạo mới.
     *
     * @param invoiceId ID của hóa đơn cần thanh toán
     * @return Thông tin link thanh toán
     */
    public PaymentLinkResponse createPaymentLink(String invoiceId) {
        String ownerId = currentUserService.getOwnerId();
        Invoice invoice = invoiceRepository.findByIdAndOwnerId(invoiceId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BadRequestException("Paid invoice cannot create a new payment link");
        }

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException("Cancelled invoice cannot create payment");
        }

        return paymentRepository.findFirstByInvoiceIdAndOwnerIdAndStatus(invoiceId, ownerId, PaymentStatus.PENDING)
                .map(PaymentLinkResponse::from)
                .orElseGet(() -> createNewPaymentLink(invoiceId, invoice));
    }

    /**
     * Khởi tạo một giao dịch thanh toán mới qua cổng PayOS.
     * Tạo mã đơn hàng (orderCode) duy nhất và gọi API PayOS để lấy link thanh toán.
     *
     * @param invoiceId ID hóa đơn
     * @param invoice Thông tin hóa đơn
     * @return Thông tin link thanh toán từ PayOS
     */
    private PaymentLinkResponse createNewPaymentLink(String invoiceId, Invoice invoice) {
        Long amount = invoice.getTotalAmount();
        if (amount == null || amount <= 0) {
            throw new BadRequestException("Invoice amount must be greater than 0");
        }

        Long orderCode = generateUniqueOrderCode();
        PayosCreatePaymentLinkRequest payosRequest = PayosCreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(amount)
                .description(buildPaymentDescription(orderCode))
                .returnUrl(payosProperties.getReturnUrl())
                .cancelUrl(payosProperties.getCancelUrl())
                .build();
        PayosCreatePaymentLinkResponse payosResponse = payosGateway.createPaymentLink(payosRequest);

        Payment payment = Payment.builder()
                .ownerId(invoice.getOwnerId())
                .invoiceId(invoiceId)
                .provider(PaymentProvider.PAYOS)
                .method(PaymentMethod.PAYOS)
                .orderCode(orderCode)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .checkoutUrl(payosResponse.getCheckoutUrl())
                .qrCode(payosResponse.getQrCode())
                .build();

        Payment saved = paymentRepository.save(payment);
        auditService.log("CREATE", "PAYMENT", saved.getId(), null, saved);
        return PaymentLinkResponse.from(saved);
    }

    /**
     * Xử lý webhook được gọi từ hệ thống PayOS khi có người quét mã thanh toán.
     * Kiểm tra chữ ký hợp lệ (signature) để chống giả mạo.
     * Xác thực số tiền thanh toán khớp với số tiền của đơn hàng.
     * Nếu thanh toán thành công, sẽ cập nhật trạng thái đơn hàng và hóa đơn thành PAID.
     * Đồng thời phát ra một sự kiện realtime (WebSocket) để cập nhật giao diện người dùng.
     *
     * @param request Dữ liệu webhook gửi từ PayOS
     * @return Bản ghi thanh toán sau khi cập nhật
     */
    public Payment handlePayosWebhook(PayosWebhookRequest request) {
        if (!payosSignatureService.isValidWebhookSignature(request)) {
            throw new BadRequestException("Invalid PayOS webhook signature");
        }

        Long orderCode = getRequiredLong(request.getData(), "orderCode");
        Payment payment = paymentRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with orderCode: " + orderCode));

        if (payment.getStatus() == PaymentStatus.PAID || payment.getStatus() == PaymentStatus.CANCELLED) {
            return payment;
        }

        Long webhookAmount = getRequiredLong(request.getData(), "amount");
        if (!webhookAmount.equals(payment.getAmount())) {
            throw new BadRequestException("Webhook amount does not match payment amount");
        }

        payment.setRawWebhookData(toRawWebhookData(request));

        InvoiceStatus invoiceStatus;
        if (isSuccessfulWebhook(request)) {
            invoiceStatus = markPaymentAndInvoicePaid(payment, request).getStatus();
        } else {
            payment.setStatus(resolveNonSuccessStatus(request));
            invoiceStatus = invoiceRepository.findByIdAndOwnerId(payment.getInvoiceId(), payment.getOwnerId())
                    .map(Invoice::getStatus)
                    .orElse(null);
        }

        Payment savedPayment = paymentRepository.save(payment);
        auditService.logWebhook("PAYMENT", savedPayment.getId(), null, savedPayment);
        realtimeEventPublisher.publishPaymentUpdated(savedPayment, invoiceStatus);
        return savedPayment;
    }

    /**
     * Lấy thông tin thanh toán theo ID.
     */
    public Payment getPaymentById(String id) {
        return paymentRepository.findByIdAndOwnerId(id, currentUserService.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }

    /**
     * Lấy lịch sử các lần tạo thanh toán của một hóa đơn.
     */
    public List<Payment> getPaymentsByInvoiceId(String invoiceId) {
        String ownerId = currentUserService.getOwnerId();
        if (invoiceRepository.findByIdAndOwnerId(invoiceId, ownerId).isEmpty()) {
            throw new ResourceNotFoundException("Invoice not found with id: " + invoiceId);
        }

        return paymentRepository.findByInvoiceIdAndOwnerId(invoiceId, ownerId);
    }

    public Payment recordCashPayment(String invoiceId, ManualPaymentRequest request) {
        return recordManualPayment(invoiceId, PaymentMethod.CASH, request);
    }

    public Payment recordBankTransferPayment(String invoiceId, ManualPaymentRequest request) {
        return recordManualPayment(invoiceId, PaymentMethod.BANK_TRANSFER, request);
    }

    /**
     * Đánh dấu bản ghi thanh toán và hóa đơn tương ứng là đã thanh toán (PAID).
     */
    private Invoice markPaymentAndInvoicePaid(Payment payment, PayosWebhookRequest request) {
        LocalDateTime paidAt = LocalDateTime.now();
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(paidAt);
        payment.setPayosTransactionId(asString(request.getData().get("reference")));
        payment.setProviderTransactionId(payment.getPayosTransactionId());

        Invoice invoice = invoiceRepository.findByIdAndOwnerId(payment.getInvoiceId(), payment.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + payment.getInvoiceId()));
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(paidAt);
            invoiceRepository.save(invoice);
        }
        return invoice;
    }

    private Payment recordManualPayment(String invoiceId, PaymentMethod method, ManualPaymentRequest request) {
        String ownerId = currentUserService.getOwnerId();
        Invoice invoice = invoiceRepository.findByIdAndOwnerId(invoiceId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BadRequestException("Paid invoice cannot be paid again");
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException("Cancelled invoice cannot be paid");
        }

        Long amount = request == null || request.getAmount() == null ? invoice.getTotalAmount() : request.getAmount();
        if (amount == null || amount <= 0) {
            throw new BadRequestException("Payment amount must be greater than 0");
        }
        if (!amount.equals(invoice.getTotalAmount())) {
            throw new BadRequestException("Payment amount must equal invoice total amount");
        }

        LocalDateTime paidAt = LocalDateTime.now();
        Payment payment = Payment.builder()
                .ownerId(ownerId)
                .invoiceId(invoiceId)
                .provider(null)
                .method(method)
                .amount(amount)
                .status(PaymentStatus.PAID)
                .note(request == null ? null : request.getNote())
                .createdBy(ownerId)
                .paidAt(paidAt)
                .build();

        // Hủy bỏ tất cả payment PENDING (PayOS) đang tồn tại cho hóa đơn này
        // để tránh data không nhất quán khi thanh toán bằng tiền mặt/chuyển khoản
        cancelPendingPayments(invoiceId, ownerId);

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(paidAt);
        invoiceRepository.save(invoice);

        Payment savedPayment = paymentRepository.save(payment);
        auditService.log("PAY", "PAYMENT", savedPayment.getId(), null, savedPayment);
        realtimeEventPublisher.publishPaymentUpdated(savedPayment, InvoiceStatus.PAID);
        return savedPayment;
    }

    /**
     * Kiểm tra xem thông tin trả về từ Webhook có báo hiệu thanh toán thành công hay không.
     */
    private boolean isSuccessfulWebhook(PayosWebhookRequest request) {
        return Boolean.TRUE.equals(request.getSuccess())
                && PAYOS_SUCCESS_CODE.equals(request.getCode())
                && PAYOS_SUCCESS_CODE.equals(asString(request.getData().get("code")));
    }

    /**
     * Xử lý trạng thái thanh toán thất bại (hủy hoặc lỗi).
     */
    private PaymentStatus resolveNonSuccessStatus(PayosWebhookRequest request) {
        String desc = (request.getDesc() + " " + asString(request.getData().get("desc")))
                .toLowerCase(Locale.ROOT);
        if (desc.contains("cancel") || desc.contains("huy")) {
            return PaymentStatus.CANCELLED;
        }

        return PaymentStatus.FAILED;
    }

    /**
     * Chuyển đổi dữ liệu webhook thành dạng Map để lưu log vào database.
     */
    private Map<String, Object> toRawWebhookData(PayosWebhookRequest request) {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("code", request.getCode());
        raw.put("desc", request.getDesc());
        raw.put("success", request.getSuccess());
        raw.put("data", request.getData());
        raw.put("signature", request.getSignature());
        return raw;
    }

    /**
     * Tạo một mã đơn hàng bằng số (orderCode) duy nhất để gửi sang PayOS.
     */
    private Long generateUniqueOrderCode() {
        for (int attempt = 0; attempt < MAX_ORDER_CODE_ATTEMPTS; attempt++) {
            Long orderCode = orderCodeGenerator.generate();
            if (!paymentRepository.existsByOrderCode(orderCode)) {
                return orderCode;
            }
        }

        throw new BadRequestException("Unable to generate unique PayOS order code");
    }

    /**
     * Tạo chuỗi mô tả thanh toán hiển thị trên màn hình chuyển khoản của ngân hàng.
     */
    private String buildPaymentDescription(Long orderCode) {
        String value = String.valueOf(orderCode);
        String suffix = value.length() <= 6 ? value : value.substring(value.length() - 6);
        return "QLT" + suffix;
    }

    /**
     * Lấy dữ liệu dạng số (Long) từ Webhook một cách an toàn.
     */
    private Long getRequiredLong(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ex) {
                throw new BadRequestException("Invalid webhook numeric field: " + key);
            }
        }

        throw new BadRequestException("Missing webhook numeric field: " + key);
    }

    /**
     * Chuyển đổi một Object thành chuỗi String.
     */
    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
    /**
     * Hủy bỏ tất cả payment đang PENDING của một hóa đơn.
     * Dùng khi thanh toán thủ công hoặc khi hóa đơn bị hủy để tránh payment cũ bị "treo".
     */
    public void cancelPendingPayments(String invoiceId, String ownerId) {
        List<Payment> pendingPayments = paymentRepository.findByInvoiceIdAndOwnerIdAndStatus(
                invoiceId, ownerId, PaymentStatus.PENDING);
        pendingPayments.forEach(p -> {
            p.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(p);
        });
    }

}
