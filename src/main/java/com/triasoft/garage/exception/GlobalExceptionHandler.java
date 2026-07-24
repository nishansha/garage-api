package com.triasoft.garage.exception;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.common.FieldError;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthenticationException(ServletWebRequest request, AuthenticationException ex) {
        return buildErrorRs("UNAUTHORIZED", ex.getMessage(), request.getRequest().getRequestURI(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(value = {BusinessException.class})
    public ResponseEntity<ApiResponse<?>> handleBusinessException(ServletWebRequest request, BusinessException exception) {
        log.error("handleBusinessException - Exception", exception);
        return buildErrorRs(exception.getCode(), exception.getMessage(), request.getRequest().getRequestURI(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {SecurityException.class})
    public ResponseEntity<ApiResponse<?>> handleSecurityException(ServletWebRequest request, SecurityException exception) {
        log.error("handleSecurityException - Exception", exception);
        HttpStatus status = exception.getError() == ErrorCode.Security.FORBIDDEN ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
        return buildErrorRs(exception.getCode(), exception.getMessage(), request.getRequest().getRequestURI(), status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(ServletWebRequest request, MethodArgumentNotValidException exception) {
        List<FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        log.warn("handleValidationException - field validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(
                ErrorCode.Validation.MISSING_REQUIRED_FIELDS.getCode(),
                ErrorCode.Validation.MISSING_REQUIRED_FIELDS.getMessage(),
                request.getRequest().getRequestURI(),
                errors));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<?>> handleOptimisticLockException(ServletWebRequest request, OptimisticLockingFailureException exception) {
        log.warn("handleOptimisticLockException - concurrent modification", exception);
        return buildErrorRs(ErrorCode.Concurrency.CONCURRENT_MODIFICATION.getCode(), ErrorCode.Concurrency.CONCURRENT_MODIFICATION.getMessage(), request.getRequest().getRequestURI(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleEntityNotFoundException(ServletWebRequest request, EntityNotFoundException exception) {
        log.warn("handleEntityNotFoundException - entity not found", exception);
        return buildErrorRs(ErrorCode.Business.RESOURCE_NOT_FOUND.getCode(), ErrorCode.Business.RESOURCE_NOT_FOUND.getMessage(), request.getRequest().getRequestURI(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrityViolationException(ServletWebRequest request, DataIntegrityViolationException exception) {
        log.error("handleDataIntegrityViolationException - data integrity violation", exception);
        return buildErrorRs(ErrorCode.General.DATA_CONFLICT.getCode(), ErrorCode.General.DATA_CONFLICT.getMessage(), request.getRequest().getRequestURI(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleUnexpectedException(ServletWebRequest request, Exception exception) {
        log.error("handleUnexpectedException - Exception", exception);
        return buildErrorRs(ErrorCode.General.GENERAL_ERROR.getCode(), ErrorCode.General.GENERAL_ERROR.getMessage(), request.getRequest().getRequestURI(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiResponse<?>> buildErrorRs(String code, String message, String path, HttpStatus status) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, message, path));
    }
}