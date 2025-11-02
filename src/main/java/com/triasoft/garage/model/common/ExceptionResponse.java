package com.triasoft.garage.model.common;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@Builder
public class ExceptionResponse {
    private String message;
    private String code;
    private String path;
    private HttpStatus status;
}
