package com.triasoft.garage.exception;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.model.common.ExceptionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ExceptionResponse> handleAuthenticationException(ServletWebRequest request, AuthenticationException ex) {
        return buildErrorRs("UNAUTHORIZED", ex.getMessage(), request.getRequest().getRequestURI(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(value = {BusinessException.class})
    public ResponseEntity<ExceptionResponse> handleBusinessException(ServletWebRequest request, BusinessException exception) {
        log.error("handleBusinessException - Exception", exception);
        return buildErrorRs(exception.getCode(), exception.getMessage(), request.getRequest().getRequestURI(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleUnexpectedException(ServletWebRequest request, Exception exception) {
        log.error("handleUnexpectedException - Exception", exception);
        return buildErrorRs(ErrorCode.General.GENERAL_ERROR.getCode(), ErrorCode.General.GENERAL_ERROR.getMessage(), request.getRequest().getRequestURI(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ExceptionResponse> buildErrorRs(String code, String message, String uri, HttpStatus status) {
        return ResponseEntity.badRequest()
                .body(ExceptionResponse.builder()
                        .code(code)
                        .message(message)
                        .path(uri)
                        .status(status)
                        .build());
    }

}
