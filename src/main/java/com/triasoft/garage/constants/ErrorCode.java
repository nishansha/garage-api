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
        PRD_NOT_FOUND("BUS_101", "Product not found"),
        PRD_BRAND_NOT_FOUND("BUS_102", "Product Brand not found"),
        PRD_MODEL_NOT_FOUND("BUS_103", "Product Model Not Found"),
        PRD_VARIANT_NOT_FOUND("BUS_104", "Product Variant Not Found"),
        CHART_OF_ACCOUNT_NOT_FOUND("BUS_105", "Expense type Not Found"),
        PURCHASE_NOT_FOUND("BUS_106", "Purchase entry not found"),
        ALREADY_SOLD("BUS_106", "Product already sold"),
        EXP_NOT_FOUNT("107", "Expense not found"),
        CHART_OF_ACCOUNT_EXIST("BUS_108", "Account already exists"),
        PRD_CATEGORY_NOT_FOUND("BUS_109", "Category Not Found"),
        PRD_CATEGORY_EXISTS("BUS_110", "Category Already Exists"),
        PRD_SEGMENT_NOT_FOUND("BUS_111", "Segment Not Found"),
        PRD_SEGMENT_EXITS("BUS_112", "Segment Already Exists"),
        PRD_BRAND_EXITS("BUS_113", "Segment Already Exists"),
        PRD_MODEL_EXITS("BUS_114", "Model Already Exists"),
        PRD_VARIENT_EXITS("BUS_115", "Variant Already Exists"),
        USER_EXISTS("BUS_116", "User Already Exists"),
        LOOKUP_EXISTS("BUS_117", "Value already exists"),
        PAYMENT_ACCOUNT_NOT_FOUND("BUS_118", "Payment account not found"),
        PAYMENT_ACCOUNT_REQUIRED("BUS_119", "Payment account is required for bank/cheque payments"),
        PAYMENT_ACCOUNT_NAME_EXISTS("BUS_120", "Payment account with this name already exists"),
        TRANSACTION_NOT_FOUND("BUS_121", "Transaction not found"),
        TRANSACTION_ALREADY_REVERSED("BUS_122", "Transaction has already been reversed"),
        OPENING_BALANCE_LOCKED("BUS_123", "Opening balance cannot be changed after transactions have been recorded. Use an adjustment transaction instead."),
        INSUFFICIENT_BALANCE("BUS_124", "Insufficient balance in the selected payment account"),
        PAYMENT_NOT_FOUND("BUS_125", "Payment record not found"),
        OVERPAYMENT("BUS_126", "Payment amount exceeds the remaining balance for this purchase"),
        PURCHASE_EXPENSE_LOCKED("BUS_127", "Expenses for this purchase are locked. The vehicle was sold and the sale month has ended.");

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
