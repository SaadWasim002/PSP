package com.upi.psp.service;

import com.upi.psp.dto.request.PaymentRequest;
import com.upi.psp.exception.PspException;
import com.upi.psp.repository.TransactionRepository;
import com.upi.psp.repository.VpaRegistryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private TransactionRepository transactionRepository;
    private VpaRegistryRepository vpaRegistryRepository;
    private VpaRegistryService vpaRegistryService;
    private BCryptPasswordEncoder passwordEncoder;
    private WebClient webClient;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        transactionRepository = Mockito.mock(TransactionRepository.class);
        vpaRegistryRepository = Mockito.mock(VpaRegistryRepository.class);
        vpaRegistryService = Mockito.mock(VpaRegistryService.class);
        passwordEncoder = new BCryptPasswordEncoder(10);
        webClient = Mockito.mock(WebClient.class);

        paymentService = new PaymentService(
                transactionRepository,
                vpaRegistryRepository,
                vpaRegistryService,
                passwordEncoder,
                webClient
        );
    }

    @Test
    void testValidatePaymentRequest_SelfPayment() {
        PaymentRequest request = PaymentRequest.builder()
                .payerVpa("saad@okaxis")
                .payeeVpa("saad@okaxis")
                .amountPaise(500L)
                .upiPin("123456")
                .build();

        PspException exception = assertThrows(PspException.class, () -> {
            paymentService.validatePaymentRequest(request);
        });

        assertEquals("SELF_PAYMENT", exception.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void testValidatePaymentRequest_InvalidPinFormat() {
        PaymentRequest request = PaymentRequest.builder()
                .payerVpa("saad@okaxis")
                .payeeVpa("riya@okhdfcbank")
                .amountPaise(500L)
                .upiPin("12345") // 5 digits
                .build();

        PspException exception = assertThrows(PspException.class, () -> {
            paymentService.validatePaymentRequest(request);
        });

        assertEquals("INVALID_PIN_FORMAT", exception.getErrorCode());
    }

    @Test
    void testParseAndValidateAmount_ValidRange() {
        Long amount = paymentService.parseAndValidateAmount(500L);
        assertEquals(500L, amount);

        amount = paymentService.parseAndValidateAmount(100L);
        assertEquals(100L, amount);

        amount = paymentService.parseAndValidateAmount(1000000L);
        assertEquals(1000000L, amount);
    }

    @Test
    void testParseAndValidateAmount_InvalidRange() {
        PspException ex1 = assertThrows(PspException.class, () -> {
            paymentService.parseAndValidateAmount(99L);
        });
        assertEquals("INVALID_AMOUNT", ex1.getErrorCode());

        PspException ex2 = assertThrows(PspException.class, () -> {
            paymentService.parseAndValidateAmount(1000001L);
        });
        assertEquals("INVALID_AMOUNT", ex2.getErrorCode());
    }

    @Test
    void testParseAndValidateAmount_FloatRejected() {
        PspException ex = assertThrows(PspException.class, () -> {
            paymentService.parseAndValidateAmount(500.50);
        });
        assertEquals("INVALID_AMOUNT", ex.getErrorCode());
    }

    @Test
    void testHashPin() {
        String pin = "123456";
        String hashed = paymentService.hashPin(pin);
        
        assertNotNull(hashed);
        assertTrue(passwordEncoder.matches(pin, hashed));
    }
}
