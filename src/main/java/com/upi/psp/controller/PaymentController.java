package com.upi.psp.controller;

import com.upi.psp.dto.request.PaymentRequest;
import com.upi.psp.dto.response.PaymentResponse;
import com.upi.psp.entity.Transaction;
import com.upi.psp.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/psp/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/pay")
    public ResponseEntity<PaymentResponse> initiatePayment(@RequestBody @Valid PaymentRequest request) {
        log.info("Received request to initiate payment from {} to {}", request.getPayerVpa(), request.getPayeeVpa());

        // 1. Validate the payment request inputs
        paymentService.validatePaymentRequest(request);

        // Parse amount
        Long amountPaise = paymentService.parseAndValidateAmount(request.getAmountPaise());

        // 2. Perform 5-minute duplicate payment check
        PaymentResponse duplicate = paymentService.checkForDuplicate(request.getPayerVpa(), request.getPayeeVpa(), amountPaise);
        if (duplicate != null) {
            log.info("Returning existing duplicate transaction for ID: {}", duplicate.getTransactionId());
            return ResponseEntity.ok(duplicate); // Return 200 OK
        }

        // 3. Process new payment
        UUID txnId = UUID.randomUUID();
        String pinHash = paymentService.hashPin(request.getUpiPin());

        // Save transaction as PENDING locally
        Transaction transaction = paymentService.createTransaction(request, txnId, amountPaise, pinHash);

        // Asynchronously forward to NPCI Switch
        paymentService.forwardToNpciAsync(transaction, pinHash);

        // Return 202 Accepted
        PaymentResponse response = PaymentResponse.builder()
                .transactionId(txnId)
                .status("PENDING")
                .payerVpa(transaction.getPayerVpa())
                .payeeVpa(transaction.getPayeeVpa())
                .amountPaise(transaction.getAmountPaise())
                .remarks(transaction.getRemarks())
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/txn/{transactionId}")
    public ResponseEntity<PaymentResponse> getTransactionStatus(@PathVariable UUID transactionId) {
        log.info("Received request to poll status for transaction ID: {}", transactionId);
        PaymentResponse response = paymentService.getTransactionStatus(transactionId);
        return ResponseEntity.ok(response);
    }
}
