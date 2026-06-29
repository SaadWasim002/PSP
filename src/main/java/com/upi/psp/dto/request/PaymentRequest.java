package com.upi.psp.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotBlank(message = "payer_vpa is required")
    @JsonProperty("payer_vpa")
    private String payerVpa;

    @NotBlank(message = "payee_vpa is required")
    @JsonProperty("payee_vpa")
    private String payeeVpa;

    @NotNull(message = "amount_paise is required")
    @JsonProperty("amount_paise")
    private Object amountPaise;

    @NotBlank(message = "upi_pin is required")
    @JsonProperty("upi_pin")
    private String upiPin;

    private String remarks;
}
