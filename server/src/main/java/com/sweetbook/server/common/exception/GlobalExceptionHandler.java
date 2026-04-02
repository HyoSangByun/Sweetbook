package com.sweetbook.server.common.exception;

import com.sweetbook.server.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage(), ex.getDetails()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ApiResponse.error(
                        ErrorCode.INVALID_INPUT.getCode(),
                        ErrorCode.INVALID_INPUT.getMessage(),
                        details
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ApiResponse.error(
                        ErrorCode.INVALID_INPUT.getCode(),
                        ErrorCode.INVALID_INPUT.getMessage(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(ErrorCode.FORBIDDEN.getStatus())
                .body(ApiResponse.error(
                        ErrorCode.FORBIDDEN.getCode(),
                        ErrorCode.FORBIDDEN.getMessage(),
                        null
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ApiResponse.error(
                        ErrorCode.INTERNAL_ERROR.getCode(),
                        ErrorCode.INTERNAL_ERROR.getMessage(),
                        null
                ));
    }
}

