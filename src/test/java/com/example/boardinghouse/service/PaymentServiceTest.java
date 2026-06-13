package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PayosGateway payosGateway;

    @Mock
    private OrderCodeGenerator orderCodeGenerator;

    private PayosSignatureService payosSignatureService;

    private PayosProperties payosProperties;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        payosProperties = new PayosProperties();
        payosProperties.setChecksumKey("checksum-key");
        payosProperties.setReturnUrl("http://localhost:3000/payment-success");
        payosProperties.setCancelUrl("http://localhost:3000/payment-cancel");

        payosSignatureService = new PayosSignatureService(payosProperties);
        paymentService = new PaymentService(
                paymentRepository,
                invoiceRepository,
                payosGateway,
                payosSignatureService,
                payosProperties,
                orderCodeGenerator
        );
    }

    @Test
    void createPaymentLinkCreatesPendingPayment() {
        when(invoiceRepository.findById("invoice-1")).thenReturn(Optional.of(invoice(InvoiceStatus.UNPAID)));
        when(paymentRepository.findFirstByInvoiceIdAndStatus("invoice-1", PaymentStatus.PENDING)).thenReturn(Optional.empty());
        when(orderCodeGenerator.generate()).thenReturn(123456789L);
        when(paymentRepository.existsByOrderCode(123456789L)).thenReturn(false);
        when(payosGateway.createPaymentLink(any(PayosCreatePaymentLinkRequest.class)))
                .thenReturn(PayosCreatePaymentLinkResponse.builder()
                        .checkoutUrl("https://pay.payos.vn/checkout")
                        .qrCode("qr-code")
                        .build());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId("payment-1");
            return payment;
        });

        PaymentLinkResponse response = paymentService.createPaymentLink("invoice-1");

        assertThat(response.getPaymentId()).isEqualTo("payment-1");
        assertThat(response.getInvoiceId()).isEqualTo("invoice-1");
        assertThat(response.getOrderCode()).isEqualTo(123456789L);
        assertThat(response.getAmount()).isEqualTo(3_035_000L);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.getCheckoutUrl()).isEqualTo("https://pay.payos.vn/checkout");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getProvider()).isEqualTo(PaymentProvider.PAYOS);
        assertThat(paymentCaptor.getValue().getQrCode()).isEqualTo("qr-code");

        ArgumentCaptor<PayosCreatePaymentLinkRequest> requestCaptor =
                ArgumentCaptor.forClass(PayosCreatePaymentLinkRequest.class);
        verify(payosGateway).createPaymentLink(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getDescription()).hasSizeLessThanOrEqualTo(9);
        assertThat(requestCaptor.getValue().getReturnUrl()).isEqualTo(payosProperties.getReturnUrl());
        assertThat(requestCaptor.getValue().getCancelUrl()).isEqualTo(payosProperties.getCancelUrl());
    }

    @Test
    void createPaymentLinkReturnsExistingPendingPayment() {
        Payment pendingPayment = payment(PaymentStatus.PENDING);
        pendingPayment.setCheckoutUrl("https://pay.payos.vn/existing");

        when(invoiceRepository.findById("invoice-1")).thenReturn(Optional.of(invoice(InvoiceStatus.UNPAID)));
        when(paymentRepository.findFirstByInvoiceIdAndStatus("invoice-1", PaymentStatus.PENDING))
                .thenReturn(Optional.of(pendingPayment));

        PaymentLinkResponse response = paymentService.createPaymentLink("invoice-1");

        assertThat(response.getCheckoutUrl()).isEqualTo("https://pay.payos.vn/existing");
        assertThat(response.getOrderCode()).isEqualTo(123456789L);
        verifyNoInteractions(orderCodeGenerator, payosGateway);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createPaymentLinkRejectsPaidInvoice() {
        when(invoiceRepository.findById("invoice-1")).thenReturn(Optional.of(invoice(InvoiceStatus.PAID)));

        assertThatThrownBy(() -> paymentService.createPaymentLink("invoice-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Paid invoice cannot create a new payment link");
    }

    @Test
    void verifiedWebhookMarksPaymentAndInvoicePaid() {
        Invoice invoice = invoice(InvoiceStatus.UNPAID);
        Payment payment = payment(PaymentStatus.PENDING);
        PayosWebhookRequest request = validWebhookRequest(123456789L, 3_035_000L);

        when(paymentRepository.findByOrderCode(123456789L)).thenReturn(Optional.of(payment));
        when(invoiceRepository.findById("invoice-1")).thenReturn(Optional.of(invoice));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.handlePayosWebhook(request);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(result.getPaidAt()).isNotNull();
        assertThat(result.getPayosTransactionId()).isEqualTo("TXN123");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.getPaidAt()).isNotNull();
        assertThat(result.getRawWebhookData()).containsEntry("code", "00");
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void invalidWebhookSignatureIsRejected() {
        PayosWebhookRequest request = validWebhookRequest(123456789L, 3_035_000L);
        request.setSignature("invalid-signature");

        assertThatThrownBy(() -> paymentService.handlePayosWebhook(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid PayOS webhook signature");

        verifyNoInteractions(paymentRepository, invoiceRepository);
    }

    @Test
    void repeatedPaidWebhookReturnsSuccessWithoutProcessingAgain() {
        Payment paidPayment = payment(PaymentStatus.PAID);
        paidPayment.setPaidAt(LocalDateTime.now().minusMinutes(5));
        PayosWebhookRequest request = validWebhookRequest(123456789L, 3_035_000L);

        when(paymentRepository.findByOrderCode(123456789L)).thenReturn(Optional.of(paidPayment));

        Payment result = paymentService.handlePayosWebhook(request);

        assertThat(result).isSameAs(paidPayment);
        verify(invoiceRepository, never()).findById(any());
        verify(invoiceRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    private Invoice invoice(InvoiceStatus status) {
        return Invoice.builder()
                .id("invoice-1")
                .totalAmount(3_035_000L)
                .status(status)
                .build();
    }

    private Payment payment(PaymentStatus status) {
        return Payment.builder()
                .id("payment-1")
                .invoiceId("invoice-1")
                .provider(PaymentProvider.PAYOS)
                .orderCode(123456789L)
                .amount(3_035_000L)
                .status(status)
                .checkoutUrl("https://pay.payos.vn/checkout")
                .qrCode("qr-code")
                .build();
    }

    private PayosWebhookRequest validWebhookRequest(Long orderCode, Long amount) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderCode", orderCode);
        data.put("amount", amount);
        data.put("description", "QLT456789");
        data.put("accountNumber", "123456789");
        data.put("reference", "TXN123");
        data.put("transactionDateTime", "2026-06-13 16:00:00");
        data.put("code", "00");
        data.put("desc", "success");

        return PayosWebhookRequest.builder()
                .code("00")
                .desc("success")
                .success(true)
                .data(data)
                .signature(payosSignatureService.createWebhookDataSignature(data))
                .build();
    }
}
