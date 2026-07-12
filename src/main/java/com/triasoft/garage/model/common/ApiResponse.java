package com.triasoft.garage.model.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String code;
    private String message;
    private String path;
    private List<FieldError> errors;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().success(true).data(data).build();
    }

    public static <T> ApiResponse<T> error(String code, String message, String path) {
        return ApiResponse.<T>builder().success(false).code(code).message(message).path(path).build();
    }

    public static <T> ApiResponse<T> error(String code, String message, String path, List<FieldError> errors) {
        return ApiResponse.<T>builder().success(false).code(code).message(message).path(path).errors(errors).build();
    }
}