package com.upi.psp.advice;

import com.upi.psp.dto.response.ErrorResponse;
import com.upi.psp.exception.PspException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PspException.class)
    public ResponseEntity<ErrorResponse> handlePspException(PspException ex) {
        log.warn("Business exception occurred: Code: {}, Message: {}", ex.getErrorCode(), ex.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .transactionId(ex.getTransactionId())
                .build();
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String firstError = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");

        log.warn("Validation failed: {}", firstError);

        String errorCode = "VALIDATION_ERROR";
        String field = ex.getBindingResult().getFieldError() != null ? ex.getBindingResult().getFieldError().getField() : "";
        if (field.contains("vpa")) {
            errorCode = "INVALID_VPA_FORMAT";
        } else if (field.contains("amount")) {
            errorCode = "INVALID_AMOUNT";
        } else if (field.contains("pin")) {
            errorCode = "INVALID_PIN_FORMAT";
        }

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(errorCode)
                .message(firstError)
                .transactionId(null)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex) {
        log.error("Unhandled exception occurred: ", ex);
        ErrorResponse response = ErrorResponse.builder()
                .errorCode("INTERNAL_ERROR")
                .message("An internal server error occurred.")
                .transactionId(null)
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
