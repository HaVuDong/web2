package com.example.boardinghouse.service;

import com.example.boardinghouse.config.PayosProperties;
import com.example.boardinghouse.dto.payment.PayosCreatePaymentLinkRequest;
import com.example.boardinghouse.dto.payment.PayosWebhookRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayosSignatureService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final PayosProperties payosProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Tạo chữ ký (signature) để đính kèm vào yêu cầu tạo link thanh toán gửi lên PayOS.
     * Đảm bảo tính toàn vẹn dữ liệu khi truyền tải qua mạng.
     *
     * @param request Dữ liệu thanh toán
     * @return Chuỗi chữ ký HMAC-SHA256
     */
    public String createPaymentRequestSignature(PayosCreatePaymentLinkRequest request) {
        String data = "amount=" + request.getAmount()
                + "&cancelUrl=" + request.getCancelUrl()
                + "&description=" + request.getDescription()
                + "&orderCode=" + request.getOrderCode()
                + "&returnUrl=" + request.getReturnUrl();
        return hmacSha256(data);
    }

    /**
     * Kiểm tra tính hợp lệ của chữ ký trong dữ liệu Webhook từ PayOS gửi về.
     * Giúp hệ thống chống lại các yêu cầu giả mạo Webhook.
     *
     * @param request Dữ liệu webhook nhận được
     * @return true nếu chữ ký hợp lệ, false nếu bị giả mạo
     */
    public boolean isValidWebhookSignature(PayosWebhookRequest request) {
        String expectedSignature = createWebhookDataSignature(request.getData());
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                request.getSignature().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Tạo chữ ký băm (hash) cho payload data nhận được từ Webhook.
     */
    public String createWebhookDataSignature(Map<String, Object> data) {
        if (data == null) {
            return hmacSha256(""); // Return hash of empty string or whatever PayOS uses
        }
        return hmacSha256(sortObjectByKey(data));
    }

    /**
     * Sắp xếp các tham số theo thứ tự alphabet của key, theo đúng tài liệu tích hợp của PayOS.
     * Dùng làm chuỗi gốc để băm (hash).
     */
    private String sortObjectByKey(Map<String, Object> data) {
        return data.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + valueToString(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    /**
     * Hàm hỗ trợ chuyển đổi an toàn các kiểu dữ liệu thành String để dùng khi tạo chuỗi mã hóa.
     */
    private String valueToString(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof Number || value instanceof Boolean || value instanceof String) {
            return String.valueOf(value);
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    /**
     * Thuật toán băm HMAC-SHA256 sử dụng khóa Checksum (Checksum Key) của PayOS.
     */
    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    payosProperties.getChecksumKey().getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );
            mac.init(secretKeySpec);
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create PayOS signature", ex);
        }
    }

    /**
     * Chuyển đổi mảng byte thành chuỗi Hex (thập lục phân).
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            hex.append(String.format("%02x", value));
        }
        return hex.toString();
    }
}
