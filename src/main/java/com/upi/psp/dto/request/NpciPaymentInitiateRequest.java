package com.upi.psp.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NpciPaymentInitiateRequest {
    @JsonProperty("transaction_id")
    private UUID transactionId;

    @JsonProperty("payer_vpa")
    private String payerVpa;

    @JsonProperty("payee_vpa")
    private String payeeVpa;

    @JsonProperty("amount_paise")
    private Long amountPaise;

    @JsonProperty("upi_pin_hash")
    private String upiPinHash;
}
