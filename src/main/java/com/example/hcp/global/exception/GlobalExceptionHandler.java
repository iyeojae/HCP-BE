package com.example.hcp.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(String code, String message) {}

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException e) {
        return ResponseEntity
                .status(e.getErrorCode().status())
                .body(new ErrorResponse(e.getErrorCode().code(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValid(MethodArgumentNotValidException e) {
        String msg = "VALIDATION_ERROR";
        if (e.getBindingResult() != null && !e.getBindingResult().getFieldErrors().isEmpty()) {
            String m = e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
            if (m != null && !m.isBlank()) msg = m;
        }

        return ResponseEntity
                .status(ErrorCode.BAD_REQUEST.status())
                .body(new ErrorResponse(ErrorCode.BAD_REQUEST.code(), msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception e) {
        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.status())
                .body(new ErrorResponse(ErrorCode.INTERNAL_ERROR.code(), "INTERNAL_ERROR"));
    }
}
