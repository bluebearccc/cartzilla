package com.cartzilla.payment.application.usecase;

import com.cartzilla.events.RabbitTopics;
import com.cartzilla.events.payment.PaymentEvents;
import com.cartzilla.payment.domain.entity.Payment;
import com.cartzilla.payment.domain.repository.PaymentRepository;
import com.cartzilla.payment.domain.vo.PaymentMethod;
import com.cartzilla.payment.domain.vo.PaymentStatus;
import com.cartzilla.payment.domain.vo.TransactionStatus;
import com.cartzilla.payment.infrastructure.vnpay.VnpayProperties;
import com.cartzilla.payment.infrastructure.vnpay.VnpayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Phủ acceptance criteria SRS: TC-20 (success), TC-21 (idempotent), TC-22 (amount mismatch). */
class VnpayCallbackUseCaseTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final VnpayProperties props = testProperties();
    private final VnpayService vnpayService = new VnpayService(props);

    private static VnpayProperties testProperties() {
        VnpayProperties properties = new VnpayProperties();
        properties.setTmnCode("TEST_TMN");
        properties.setHashSecret("test-only-hmac-secret");
        return properties;
    }
    private final VnpayCallbackUseCase useCase = new VnpayCallbackUseCase(
            paymentRepository, vnpayService, rabbit, new ObjectMapper());

    private Map<String, String> signedParams(String txnRef, String amount, String code, String txnNo) {
        Map<String, String> p = new HashMap<>();
        p.put("vnp_TxnRef", txnRef);
        p.put("vnp_Amount", amount);
        p.put("vnp_ResponseCode", code);
        p.put("vnp_TransactionStatus", "00".equals(code) ? "00" : "02");
        p.put("vnp_TransactionNo", txnNo);
        p.put("vnp_TmnCode", props.getTmnCode());
        p.put("vnp_SecureHash", vnpayService.sign(p));
        return p;
    }

    private Payment vnpayPayment(BigDecimal amount, String ref) {
        Payment p = Payment.create(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.VNPAY, amount);
        p.attachVnpayRef(ref);
        return p;
    }

    @Test
    void tc20_successCallback_marksPaidAndPublishesResult() {
        Payment payment = vnpayPayment(new BigDecimal("100000"), "VNP123");
        when(paymentRepository.existsTransactionByProviderTxnRef("VNPAY", "TXN1")).thenReturn(false);
        when(paymentRepository.findByVnpayTxnRef("VNP123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VnpayCallbackUseCase.Result r = useCase.execute(
                signedParams("VNP123", "10000000", "00", "TXN1"));

        assertTrue(r.success());
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        verify(rabbit).convertAndSend(eq(RabbitTopics.PAYMENT_EXCHANGE),
                eq(RabbitTopics.RK_PAYMENT_RESULT), any(PaymentEvents.PaymentResultEvent.class));
    }

    @Test
    void tc21_duplicateCallback_isIdempotent() {
        Payment paid = vnpayPayment(new BigDecimal("100000"), "VNP123");
        paid.settleVnpaySuccess("TXN1", "{}"); // đã PAID từ callback trước
        when(paymentRepository.existsTransactionByProviderTxnRef("VNPAY", "TXN1")).thenReturn(true);
        when(paymentRepository.findByVnpayTxnRef("VNP123")).thenReturn(Optional.of(paid));

        VnpayCallbackUseCase.Result r = useCase.execute(
                signedParams("VNP123", "10000000", "00", "TXN1"));

        assertTrue(r.idempotent());
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(rabbit);
    }

    @Test
    void tc22_amountMismatch_marksFailed() {
        Payment payment = vnpayPayment(new BigDecimal("100000"), "VNP123");
        when(paymentRepository.existsTransactionByProviderTxnRef(anyString(), anyString())).thenReturn(false);
        when(paymentRepository.findByVnpayTxnRef("VNP123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // callback báo 50.000 trong khi order là 100.000
        VnpayCallbackUseCase.Result r = useCase.execute(
                signedParams("VNP123", "5000000", "00", "TXN1"));

        assertFalse(r.success());
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
    }

    @Test
    void cancelledAtVnpay_code24_recordsNormalizedFailureWithoutOversizedStatus() {
        Payment payment = vnpayPayment(new BigDecimal("100000"), "VNP123");
        when(paymentRepository.existsTransactionByProviderTxnRef("VNPAY", "TXN24")).thenReturn(false);
        when(paymentRepository.findByVnpayTxnRef("VNP123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VnpayCallbackUseCase.Result result = useCase.execute(
                signedParams("VNP123", "10000000", "24", "TXN24"));

        assertFalse(result.success());
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        var transaction = payment.getTransactions().get(payment.getTransactions().size() - 1);
        assertEquals(TransactionStatus.FAILED, transaction.getStatus());
        assertEquals("VNPAY_CODE_24", transaction.getErrorCode());
        assertEquals("VNPay callback failed: VNPAY_CODE_24", transaction.getErrorMessage());
        assertTrue(transaction.getStatus().name().length() <= 20);
    }

    @Test
    void responseCode00_butTransactionNotSuccessful_isRejected() {
        Payment payment = vnpayPayment(new BigDecimal("100000"), "VNP123");
        when(paymentRepository.existsTransactionByProviderTxnRef("VNPAY", "TXN2")).thenReturn(false);
        when(paymentRepository.findByVnpayTxnRef("VNP123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Map<String, String> params = signedParams("VNP123", "10000000", "00", "TXN2");
        params.put("vnp_TransactionStatus", "02");
        params.put("vnp_SecureHash", vnpayService.sign(params));

        VnpayCallbackUseCase.Result result = useCase.execute(params);

        assertFalse(result.success());
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        var transaction = payment.getTransactions().get(payment.getTransactions().size() - 1);
        assertEquals("VNPAY_STATUS_02", transaction.getErrorCode());
    }

    @Test
    void invalidSignature_isRejected() {
        Map<String, String> params = signedParams("VNP123", "10000000", "00", "TXN1");
        params.put("vnp_SecureHash", "deadbeef"); // hỏng chữ ký

        VnpayCallbackUseCase.Result r = useCase.execute(params);

        assertFalse(r.success());
        assertEquals("97", r.code());
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void laterFailureCannotDowngradePaidPayment() {
        Payment payment = vnpayPayment(new BigDecimal("100000"), "VNP123");
        payment.settleVnpaySuccess("TXN1", "{}");
        when(paymentRepository.findByVnpayTxnRef("VNP123")).thenReturn(Optional.of(payment));
        when(paymentRepository.existsTransactionByProviderTxnRef("VNPAY", "TXN2")).thenReturn(false);

        VnpayCallbackUseCase.Result result = useCase.execute(
                signedParams("VNP123", "10000000", "24", "TXN2"));

        assertTrue(result.idempotent());
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(rabbit);
    }
}
