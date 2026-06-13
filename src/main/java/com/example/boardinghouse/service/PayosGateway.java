package com.example.boardinghouse.service;

import com.example.boardinghouse.dto.payment.PayosCreatePaymentLinkRequest;
import com.example.boardinghouse.dto.payment.PayosCreatePaymentLinkResponse;

public interface PayosGateway {
    PayosCreatePaymentLinkResponse createPaymentLink(PayosCreatePaymentLinkRequest request);
}
