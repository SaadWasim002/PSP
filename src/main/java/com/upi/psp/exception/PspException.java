package com.upi.psp.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@Getter
public class PspException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;
    private final UUID transactionId;

    public PspException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
        this.transactionId = null;
    }

    public PspException(String errorCode, String message, HttpStatus status, UUID transactionId) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
        this.transactionId = transactionId;
    }
}
