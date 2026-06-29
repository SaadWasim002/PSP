package com.upi.psp.controller;

import com.upi.psp.dto.request.VpaRegisterRequest;
import com.upi.psp.dto.response.BalanceResponse;
import com.upi.psp.dto.response.VpaRegisterResponse;
import com.upi.psp.service.VpaRegistryService;
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

@Slf4j
@RestController
@RequestMapping("/api/vpa")
@RequiredArgsConstructor
public class VpaRegistryController {

    private final VpaRegistryService vpaRegistryService;

    @PostMapping("/register")
    public ResponseEntity<VpaRegisterResponse> registerVpa(@RequestBody @Valid VpaRegisterRequest request) {
        log.info("Received VPA registration request for VPA: {}", request.getVpa());
        VpaRegisterResponse response = vpaRegistryService.registerVpa(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{vpa}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String vpa) {
        log.info("Received balance check request for VPA: {}", vpa);
        BalanceResponse response = vpaRegistryService.getBalance(vpa);
        return ResponseEntity.ok(response);
    }
}
