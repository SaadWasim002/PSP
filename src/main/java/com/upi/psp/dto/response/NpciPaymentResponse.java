package com.upi.psp.dto.response;

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
public class NpciPaymentResponse {
    @JsonProperty("transaction_id")
    private UUID transactionId;
    private String status;
    @JsonProperty("payer_rrn")
    private String payerRrn;
    @JsonProperty("payee_rrn")
    private String payeeRrn;
    @JsonProperty("failure_reason")
    private String failureReason;
}
