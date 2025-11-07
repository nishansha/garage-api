package com.triasoft.garage.constants;

import lombok.Getter;

public class ErrorCode {

    @Getter
    public enum General implements Errors {
        GENERAL_ERROR("GEN_100", "Internal Server Error");

        private final String code;
        private final String message;

        General(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    @Getter
    public enum Validation implements Errors {
        FIELD_ERROR("FLD_100", "Invalid Field"),
        MISSING_REQUIRED_FIELDS("FLD_102", "Required fields are Missing");
        private final String code;
        private final String message;

        Validation(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    @Getter
    public enum Security implements Errors {
        BREACH("SEC_100", "Security breach detected"),
        INVALID_ACCESS("SEC_101", "Invalid Resource Access"),
        INVALID_TOKEN("SEC_102", "Invalid Token"),
        TOKEN_EXPIRED("SEC_103", "Token Expired");
        private final String code;
        private final String message;

        Security(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    @Getter
    public enum Business implements Errors {
        USER_NOT_FOUND("BUS_100", "Invalid Credentials"),
        PRD_NOT_FOUND("BUS_101", "Product not found");

        private final String code;
        private final String message;

        Business(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    @Getter
    public static class CustomError implements Errors {

        private final String code;
        private final String message;

        public CustomError(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }

}
