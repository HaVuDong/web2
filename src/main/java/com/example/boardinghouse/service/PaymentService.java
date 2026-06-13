package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.common.util.OrderCodeGenerator;
import com.example.boardinghouse.config.PayosProperties;
import com.example.boardinghouse.domain.entity.Invoice;
import com.example.boardinghouse.domain.entity.Payment;
import com.example.boardinghouse.domain.enums.InvoiceStatus;
import com.example.boardinghouse.domain.enums.PaymentProvider;
import com.example.boardinghouse.domain.enums.PaymentStatus;
import com.example.boardinghouse.dto.payment.PaymentLinkResponse;
import com.example.boardinghouse.dto.payment.PayosCreatePaymentLinkRequest;
import com.example.boardinghouse.dto.payment.PayosCreatePaymentLinkResponse;
import com.example.boardinghouse.dto.payment.PayosWebhookRequest;
import com.example.boardinghouse.repository.InvoiceRepository;
import com.example.boardinghouse.repository.PaymentRepository;
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

    public PaymentLinkResponse createPaymentLink(String invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BadRequestException("Paid invoice cannot create a new payment link");
        }

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException("Cancelled invoice cannot create payment");
        }

        return paymentRepository.findFirstByInvoiceIdAndStatus(invoiceId, PaymentStatus.PENDING)
                .map(PaymentLinkResponse::from)
                .orElseGet(() -> createNewPaymentLink(invoiceId, invoice));
    }

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
                .invoiceId(invoiceId)
                .provider(PaymentProvider.PAYOS)
                .orderCode(orderCode)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .checkoutUrl(payosResponse.getCheckoutUrl())
                .qrCode(payosResponse.getQrCode())
                .build();

        return PaymentLinkResponse.from(paymentRepository.save(payment));
    }

    public Payment handlePayosWebhook(PayosWebhookRequest request) {
        if (!payosSignatureService.isValidWebhookSignature(request)) {
            throw new BadRequestException("Invalid PayOS webhook signature");
        }

        Long orderCode = getRequiredLong(request.getData(), "orderCode");
        Payment payment = paymentRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with orderCode: " + orderCode));

        if (payment.getStatus() == PaymentStatus.PAID) {
            return payment;
        }

        Long webhookAmount = getRequiredLong(request.getData(), "amount");
        if (!webhookAmount.equals(payment.getAmount())) {
            throw new BadRequestException("Webhook amount does not match payment amount");
        }

        payment.setRawWebhookData(toRawWebhookData(request));

        if (isSuccessfulWebhook(request)) {
            markPaymentAndInvoicePaid(payment, request);
        } else {
            payment.setStatus(resolveNonSuccessStatus(request));
        }

        return paymentRepository.save(payment);
    }

    public Payment getPaymentById(String id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }

    public List<Payment> getPaymentsByInvoiceId(String invoiceId) {
        if (!invoiceRepository.existsById(invoiceId)) {
            throw new ResourceNotFoundException("Invoice not found with id: " + invoiceId);
        }

        return paymentRepository.findByInvoiceId(invoiceId);
    }

    private void markPaymentAndInvoicePaid(Payment payment, PayosWebhookRequest request) {
        LocalDateTime paidAt = LocalDateTime.now();
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(paidAt);
        payment.setPayosTransactionId(asString(request.getData().get("reference")));

        Invoice invoice = invoiceRepository.findById(payment.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + payment.getInvoiceId()));
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(paidAt);
            invoiceRepository.save(invoice);
        }
    }

    private boolean isSuccessfulWebhook(PayosWebhookRequest request) {
        return Boolean.TRUE.equals(request.getSuccess())
                && PAYOS_SUCCESS_CODE.equals(request.getCode())
                && PAYOS_SUCCESS_CODE.equals(asString(request.getData().get("code")));
    }

    private PaymentStatus resolveNonSuccessStatus(PayosWebhookRequest request) {
        String desc = (request.getDesc() + " " + asString(request.getData().get("desc")))
                .toLowerCase(Locale.ROOT);
        if (desc.contains("cancel") || desc.contains("huy")) {
            return PaymentStatus.CANCELLED;
        }

        return PaymentStatus.FAILED;
    }

    private Map<String, Object> toRawWebhookData(PayosWebhookRequest request) {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("code", request.getCode());
        raw.put("desc", request.getDesc());
        raw.put("success", request.getSuccess());
        raw.put("data", request.getData());
        raw.put("signature", request.getSignature());
        return raw;
    }

    private Long generateUniqueOrderCode() {
        for (int attempt = 0; attempt < MAX_ORDER_CODE_ATTEMPTS; attempt++) {
            Long orderCode = orderCodeGenerator.generate();
            if (!paymentRepository.existsByOrderCode(orderCode)) {
                return orderCode;
            }
        }

        throw new BadRequestException("Unable to generate unique PayOS order code");
    }

    private String buildPaymentDescription(Long orderCode) {
        String value = String.valueOf(orderCode);
        String suffix = value.length() <= 6 ? value : value.substring(value.length() - 6);
        return "QLT" + suffix;
    }

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

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

}
