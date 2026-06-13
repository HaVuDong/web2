package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.config.PayosProperties;
import com.example.boardinghouse.dto.payment.PayosCreatePaymentLinkRequest;
import com.example.boardinghouse.dto.payment.PayosCreatePaymentLinkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PayosRestGateway implements PayosGateway {

    private final RestClient payosRestClient;
    private final PayosProperties payosProperties;
    private final PayosSignatureService payosSignatureService;

    @Override
    public PayosCreatePaymentLinkResponse createPaymentLink(PayosCreatePaymentLinkRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderCode", request.getOrderCode());
        body.put("amount", request.getAmount());
        body.put("description", request.getDescription());
        body.put("cancelUrl", request.getCancelUrl());
        body.put("returnUrl", request.getReturnUrl());
        body.put("signature", payosSignatureService.createPaymentRequestSignature(request));

        try {
            Map<String, Object> response = payosRestClient.post()
                    .uri("/v2/payment-requests")
                    .header("x-client-id", payosProperties.getClientId())
                    .header("x-api-key", payosProperties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            return toPaymentLinkResponse(response);
        } catch (RestClientException ex) {
            throw new BadRequestException("Unable to create PayOS payment link");
        }
    }

    private PayosCreatePaymentLinkResponse toPaymentLinkResponse(Map<String, Object> response) {
        if (response == null) {
            throw new BadRequestException("PayOS returned an empty response");
        }

        String code = asString(response.get("code"));
        if (!"00".equals(code)) {
            throw new BadRequestException("PayOS create payment link failed: " + asString(response.get("desc")));
        }

        Object data = response.get("data");
        if (!(data instanceof Map<?, ?> dataMap)) {
            throw new BadRequestException("PayOS returned an invalid payment link response");
        }

        return PayosCreatePaymentLinkResponse.builder()
                .checkoutUrl(asString(dataMap.get("checkoutUrl")))
                .qrCode(asString(dataMap.get("qrCode")))
                .build();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
