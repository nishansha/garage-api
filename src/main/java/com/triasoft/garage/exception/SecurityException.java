package com.triasoft.garage.exception;

import com.triasoft.garage.constants.Errors;
import lombok.Getter;

import java.io.Serial;

@Getter
public class SecurityException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -3308601811449638366L;

    private final Errors error;

    public SecurityException(Errors error) {
        super(error.getMessage());
        this.error = error;
    }

    public String getCode() {
        return error.getCode();
    }

    @Override
    public String getMessage() {
        return error.getMessage();
    }
}
