package com.upi.psp.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VpaRegisterRequest {
    @NotBlank(message = "vpa is required")
    private String vpa;

    @NotNull(message = "account_id is required")
    @JsonProperty("account_id")
    private UUID accountId;

    @NotBlank(message = "account_holder is required")
    @JsonProperty("account_holder")
    private String accountHolder;
}
