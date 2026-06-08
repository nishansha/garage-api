package com.triasoft.garage.exception;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.model.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleUnexpectedException(ServletWebRequest request, Exception exception) {
        log.error("handleUnexpectedException - Exception", exception);
        return buildErrorRs(ErrorCode.General.GENERAL_ERROR.getCode(), ErrorCode.General.GENERAL_ERROR.getMessage(), request.getRequest().getRequestURI(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiResponse<?>> buildErrorRs(String code, String message, String path, HttpStatus status) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, message, path));
    }
}