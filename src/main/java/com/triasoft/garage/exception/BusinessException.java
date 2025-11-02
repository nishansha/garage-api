package com.triasoft.garage.exception;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.constants.Errors;
import lombok.Getter;

import java.io.Serial;

/**
 * @author Yellin
 * @since 2025-03-14
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1734981069497374959L;

    private final Errors error;

    public BusinessException(Errors errorCode) {
        super(errorCode.getMessage());
        this.error = errorCode;
    }

    public BusinessException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.error = new ErrorCode.CustomError(errorCode, errorMessage);
    }

    public BusinessException(String message) {
        super(message);
        this.error = new ErrorCode.CustomError(ErrorCode.General.GENERAL_ERROR.getCode(), message);
    }

    public String getCode() {
        return error.getCode();
    }

    @Override
    public String getMessage() {
        return error.getMessage();
    }

}
