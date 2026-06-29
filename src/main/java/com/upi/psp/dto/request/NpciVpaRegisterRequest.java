package com.upi.psp.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NpciVpaRegisterRequest {
    private String vpa;
    private String bankCode;
    private String bankApiUrl;
    private String accountNumber;
}
