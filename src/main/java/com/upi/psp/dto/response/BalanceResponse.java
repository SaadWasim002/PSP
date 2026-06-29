package com.upi.psp.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceResponse {
    private String vpa;
    
    @JsonProperty("balance_paise")
    private Long balancePaise;

    @JsonProperty("daily_limit_paise")
    private Long dailyLimitPaise;

    @JsonProperty("daily_used_paise")
    private Long dailyUsedPaise;

    @JsonProperty("pin_locked")
    private Boolean pinLocked;
}
