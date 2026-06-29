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
public class ErrorResponse {
    @JsonProperty("error_code")
    private String errorCode;
    private String message;
    @JsonProperty("transaction_id")
    private UUID transactionId;
}
