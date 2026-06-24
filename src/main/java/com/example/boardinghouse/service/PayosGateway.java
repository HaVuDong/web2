package com.example.boardinghouse.service;

import com.example.boardinghouse.dto.payment.PayosCreatePaymentLinkRequest;
import com.example.boardinghouse.dto.payment.PayosCreatePaymentLinkResponse;

public interface PayosGateway {
    /**
     * Gửi yêu cầu tạo link thanh toán (checkout link) lên PayOS.
     *
     * @param request Thông tin yêu cầu thanh toán
     * @return Dữ liệu phản hồi chứa URL thanh toán và mã QR
     */
    PayosCreatePaymentLinkResponse createPaymentLink(PayosCreatePaymentLinkRequest request);
}
