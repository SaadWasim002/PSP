package com.upi.psp.service;

import com.upi.psp.dto.request.NpciVpaRegisterRequest;
import com.upi.psp.dto.request.VpaRegisterRequest;
import com.upi.psp.dto.response.BalanceResponse;
import com.upi.psp.dto.response.VpaRegisterResponse;
import com.upi.psp.entity.VpaRegistry;
import com.upi.psp.exception.PspException;
import com.upi.psp.repository.VpaRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class VpaRegistryService {

    private final VpaRegistryRepository vpaRegistryRepository;
    private final WebClient webClient;

    @Value("${npci.api.url}")
    private String npciApiUrl;

    @Value("${bank.api.url}")
    private String bankApiUrl;

    @Transactional
    public VpaRegisterResponse registerVpa(VpaRegisterRequest request) {
        String vpa = request.getVpa().toLowerCase();
        log.info("Processing VPA registration for VPA: {}", request.getVpa());

        // 1. Validate VPA format and handles
        validateVpa(request.getVpa());

        // 2. Check duplicate registration
        if (vpaRegistryRepository.existsById(vpa)) {
            throw new PspException("VPA_ALREADY_EXISTS", "VPA already registered: " + request.getVpa(), HttpStatus.CONFLICT);
        }

        // 3. Forward to NPCI Switch
        String bankCode = extractBankCode(vpa);
        NpciVpaRegisterRequest npciRequest = NpciVpaRegisterRequest.builder()
                .vpa(vpa)
                .bankCode(bankCode)
                .bankApiUrl(bankApiUrl)
                .accountNumber(request.getAccountId().toString())
                .build();

        log.info("Forwarding VPA registration to NPCI Switch at: {}/switch/vpa/register", npciApiUrl);
        try {
            webClient.post()
                    .uri(npciApiUrl + "/switch/vpa/register")
                    .bodyValue(npciRequest)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.error("Failed to register VPA on NPCI Switch: ", e);
            throw new PspException("NPCI_UNAVAILABLE", "NPCI Switch didn't respond. Client should retry after delay.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        // 4. Save locally
        VpaRegistry localRegistry = VpaRegistry.builder()
                .vpa(vpa)
                .accountId(request.getAccountId())
                .accountHolder(request.getAccountHolder())
                .build();
        vpaRegistryRepository.save(localRegistry);

        return VpaRegisterResponse.builder()
                .status("SUCCESS")
                .message("VPA registered successfully")
                .vpa(vpa)
                .build();
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String vpaStr) {
        log.info("Processing balance check for VPA: {}", vpaStr);
        String vpa = vpaStr.toLowerCase();

        // 1. Validate VPA format
        validateVpa(vpaStr);

        // 2. Check if registered locally
        if (!vpaRegistryRepository.existsById(vpa)) {
            throw new PspException("VPA_NOT_FOUND", "One or both VPAs don't exist in psp.vpa_registry. Specifying vpa: " + vpaStr, HttpStatus.NOT_FOUND);
        }

        // 3. Query Bank Service
        log.info("Querying bank balance for VPA: {} at bank URL: {}", vpa, bankApiUrl);
        try {
            return webClient.get()
                    .uri(bankApiUrl + "/bank/account/" + vpa + "/balance")
                    .retrieve()
                    .bodyToMono(BalanceResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to query bank balance for VPA: {}", vpa, e);
            throw new PspException("NPCI_UNAVAILABLE", "Bank service unavailable. Client should retry after delay.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public void validateVpa(String vpa) {
        if (vpa == null || !vpa.matches("^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+$")) {
            throw new PspException("INVALID_VPA_FORMAT", "VPA doesn't match regex. Invalid VPA: " + vpa, HttpStatus.BAD_REQUEST);
        }
        String handle = vpa.substring(vpa.lastIndexOf("@") + 1).toLowerCase();
        if (!handle.equals("okaxis") && !handle.equals("okhdfcbank") && !handle.equals("oksbi")
                && !handle.equals("okicici") && !handle.equals("ybl") && !handle.equals("upi")) {
            throw new PspException("INVALID_VPA_FORMAT", "VPA doesn't match regex. Invalid VPA handle: " + handle, HttpStatus.BAD_REQUEST);
        }
    }

    private String extractBankCode(String vpa) {
        String handle = vpa.substring(vpa.lastIndexOf("@") + 1).toLowerCase();
        return switch (handle) {
            case "okaxis" -> "AXIS";
            case "okhdfcbank" -> "HDFC";
            case "oksbi" -> "SBI";
            case "okicici" -> "ICICI";
            case "ybl" -> "YBL";
            case "upi" -> "UPI";
            default -> throw new PspException("INVALID_VPA_FORMAT", "VPA doesn't match regex. Unsupported VPA handle: " + handle, HttpStatus.BAD_REQUEST);
        };
    }
}
