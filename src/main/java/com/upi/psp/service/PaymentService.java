package com.upi.psp.service;

import com.upi.psp.dto.request.NpciPaymentInitiateRequest;
import com.upi.psp.dto.request.PaymentRequest;
import com.upi.psp.dto.response.NpciPaymentResponse;
import com.upi.psp.dto.response.PaymentResponse;
import com.upi.psp.entity.Transaction;
import com.upi.psp.exception.PspException;
import com.upi.psp.repository.TransactionRepository;
import com.upi.psp.repository.VpaRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final VpaRegistryRepository vpaRegistryRepository;
    private final VpaRegistryService vpaRegistryService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final WebClient webClient;

    @Value("${npci.api.url}")
    private String npciApiUrl;

    public void validatePaymentRequest(PaymentRequest request) {
        String payerVpa = request.getPayerVpa();
        String payeeVpa = request.getPayeeVpa();

        // 1. VPA regex and handles check
        vpaRegistryService.validateVpa(payerVpa);
        vpaRegistryService.validateVpa(payeeVpa);

        // 2. Self-payment check
        if (payerVpa.equalsIgnoreCase(payeeVpa)) {
            throw new PspException("SELF_PAYMENT", "Payer VPA cannot be the same as payee VPA: " + payerVpa, HttpStatus.BAD_REQUEST);
        }

        // 3. Amount format and 4. Range check
        parseAndValidateAmount(request.getAmountPaise());

        // 5. PIN format
        String upiPin = request.getUpiPin();
        if (upiPin == null || !upiPin.matches("^\\d{6}$")) {
            throw new PspException("INVALID_PIN_FORMAT", "PIN is not exactly 6 digits", HttpStatus.BAD_REQUEST);
        }

        // 6. both VPAs exist check
        if (!vpaRegistryRepository.existsById(payerVpa.toLowerCase())) {
            throw new PspException("VPA_NOT_FOUND", "Payer VPA does not exist in registry: " + payerVpa, HttpStatus.NOT_FOUND);
        }
        if (!vpaRegistryRepository.existsById(payeeVpa.toLowerCase())) {
            throw new PspException("VPA_NOT_FOUND", "Payee VPA does not exist in registry: " + payeeVpa, HttpStatus.NOT_FOUND);
        }
    }

    public PaymentResponse checkForDuplicate(String payerVpa, String payeeVpa, Long amountPaise) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        List<Transaction> list = transactionRepository.findDuplicateTransactions(
                payerVpa.toLowerCase(), payeeVpa.toLowerCase(), amountPaise, cutoff);
        if (!list.isEmpty()) {
            Transaction existing = list.get(0);
            log.info("Duplicate transaction detected within 5 min window for transaction ID: {}. Status: {}",
                    existing.getTransactionId(), existing.getStatus());
            return PaymentResponse.builder()
                    .transactionId(existing.getTransactionId())
                    .status(existing.getStatus())
                    .payerVpa(existing.getPayerVpa())
                    .payeeVpa(existing.getPayeeVpa())
                    .amountPaise(existing.getAmountPaise())
                    .remarks(existing.getRemarks())
                    .build();
        }
        return null;
    }

    public Long parseAndValidateAmount(Object amountObj) {
        if (amountObj == null) {
            throw new PspException("INVALID_AMOUNT", "Amount is required", HttpStatus.BAD_REQUEST);
        }

        Long amount;
        if (amountObj instanceof Integer val) {
            amount = val.longValue();
        } else if (amountObj instanceof Long val) {
            amount = val;
        } else if (amountObj instanceof Double || amountObj instanceof Float) {
            throw new PspException("INVALID_AMOUNT", "Not an integer, or outside 100-1000000 range", HttpStatus.BAD_REQUEST);
        } else {
            try {
                String strVal = amountObj.toString();
                if (strVal.contains(".")) {
                    throw new PspException("INVALID_AMOUNT", "Not an integer, or outside 100-1000000 range", HttpStatus.BAD_REQUEST);
                }
                amount = Long.parseLong(strVal);
            } catch (PspException pe) {
                throw pe;
            } catch (Exception e) {
                throw new PspException("INVALID_AMOUNT", "Not an integer, or outside 100-1000000 range", HttpStatus.BAD_REQUEST);
            }
        }

        if (amount < 100 || amount > 1000000) {
            throw new PspException("INVALID_AMOUNT", "Not an integer, or outside 100-1000000 range", HttpStatus.BAD_REQUEST);
        }

        return amount;
    }

    @Transactional
    public Transaction createTransaction(PaymentRequest request, UUID txnId, Long amountPaise, String pinHash) {
        Transaction transaction = Transaction.builder()
                .transactionId(txnId)
                .payerVpa(request.getPayerVpa().toLowerCase())
                .payeeVpa(request.getPayeeVpa().toLowerCase())
                .amountPaise(amountPaise)
                .status("PENDING")
                .remarks(request.getRemarks())
                .build();
        return transactionRepository.save(transaction);
    }

    @Async
    public void forwardToNpciAsync(Transaction transaction, String upiPinHash) {
        log.info("Starting async forward to NPCI for transaction ID: {}", transaction.getTransactionId());

        NpciPaymentInitiateRequest npciRequest = NpciPaymentInitiateRequest.builder()
                .transactionId(transaction.getTransactionId())
                .payerVpa(transaction.getPayerVpa())
                .payeeVpa(transaction.getPayeeVpa())
                .amountPaise(transaction.getAmountPaise())
                .upiPinHash(upiPinHash)
                .build();

        try {
            log.info("Posting payment initiation to NPCI Switch at: {}/switch/initiate", npciApiUrl);
            NpciPaymentResponse response = webClient.post()
                    .uri(npciApiUrl + "/switch/initiate")
                    .bodyValue(npciRequest)
                    .retrieve()
                    .bodyToMono(NpciPaymentResponse.class)
                    .block();

            if (response != null) {
                log.info("NPCI Switch response for transaction ID {}: {}", transaction.getTransactionId(), response.getStatus());
                // Update transaction status locally
                transaction.setStatus(response.getStatus());
                transactionRepository.save(transaction);
            }
        } catch (Exception e) {
            log.error("Failed to call NPCI Switch for transaction ID: {}", transaction.getTransactionId(), e);
        }
    }

    public String hashPin(String rawPin) {
        return passwordEncoder.encode(rawPin);
    }

    @Transactional
    public PaymentResponse getTransactionStatus(UUID transactionId) {
        log.info("Processing status poll for transaction ID: {}", transactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new PspException("TRANSACTION_NOT_FOUND", "Transaction not found", HttpStatus.NOT_FOUND, transactionId));

        // If not terminal, sync with NPCI Switch
        if ("PENDING".equalsIgnoreCase(transaction.getStatus()) || "INITIATED".equalsIgnoreCase(transaction.getStatus())) {
            log.info("Transaction {} is pending. Fetching latest status from NPCI Switch.", transactionId);
            try {
                NpciPaymentResponse npciResponse = webClient.get()
                        .uri(npciApiUrl + "/switch/txn/" + transactionId)
                        .retrieve()
                        .bodyToMono(NpciPaymentResponse.class)
                        .block();

                if (npciResponse != null && npciResponse.getStatus() != null) {
                    log.info("NPCI Switch status for transaction ID {}: {}", transactionId, npciResponse.getStatus());
                    transaction.setStatus(npciResponse.getStatus());
                    transactionRepository.save(transaction);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch transaction status from NPCI Switch for ID: {}. Returning local status.", transactionId, e);
            }
        }

        return PaymentResponse.builder()
                .transactionId(transaction.getTransactionId())
                .status(transaction.getStatus())
                .payerVpa(transaction.getPayerVpa())
                .payeeVpa(transaction.getPayeeVpa())
                .amountPaise(transaction.getAmountPaise())
                .remarks(transaction.getRemarks())
                .build();
    }
}
