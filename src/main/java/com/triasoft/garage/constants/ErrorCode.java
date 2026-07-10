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
        TOKEN_EXPIRED("SEC_103", "Token Expired"),
        REFRESH_TOKEN_REUSED("SEC_104", "Refresh token reuse detected; session revoked"),
        SESSION_TERMINATED("SEC_105", "Session ended; signed in on another device");
        private final String code;
        private final String message;

        Security(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    @Getter
    public enum Concurrency implements Errors {
        CONCURRENT_MODIFICATION("CON_100", "This record was changed by another operation. Please reload and try again.");
        private final String code;
        private final String message;

        Concurrency(String code, String message) {
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
        PRD_BRAND_EXITS("BUS_113", "Brand Already Exists"),
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
        PURCHASE_EXPENSE_LOCKED("BUS_127", "Expenses for this purchase are locked. The vehicle was sold and the sale month has ended."),
        DIRECT_ENTRY_NOT_FOUND("BUS_128", "Direct entry not found"),
        DIRECT_ENTRY_ALREADY_REVERSED("BUS_129", "Direct entry has already been reversed"),
        JOURNAL_COA_MISSING("BUS_130", "Required CoA account is missing from the chart of accounts"),
        JOURNAL_NOT_BALANCED("BUS_131", "Journal entry is not balanced: total debits must equal total credits"),
        JOURNAL_ALREADY_POSTED("BUS_132", "A journal entry already exists for this reference"),
        JOURNAL_PAYMENT_ACCOUNT_COA_MISSING("BUS_133", "Payment account is not linked to a chart of accounts entry"),
        SALE_RETURN_NOT_FOUND("BUS_140", "Sale return not found"),
        SALE_ALREADY_RETURNED("BUS_141", "Sale has already been returned"),
        SALE_RETURN_FINANCED_NOT_ALLOWED("BUS_142", "Financed sales cannot be returned"),
        EXCHANGE_HANDLING_INVALID("BUS_143", "Exchange handling mode is invalid for this sale"),
        EXCHANGE_BUYBACK_AMOUNT_INVALID("BUS_144", "Exchange buyback amount is required only for KEEP_AND_BUYBACK"),
        EXCHANGE_BUYBACK_EXCEEDS_ORIGINAL("BUS_145", "Exchange buyback amount cannot exceed original exchange amount"),
        EXCHANGE_DEDUCTION_NOT_ALLOWED("BUS_146", "Exchange-vehicle deductions are only allowed for KEEP_AND_BUYBACK"),
        EXCHANGE_VEHICLE_ALREADY_SOLD("BUS_147", "Exchange vehicle has already been sold; cannot return it to buyer"),
        DEDUCTION_EXCEEDS_BASE("BUS_148", "Deduction exceeds the base amount it applies to"),
        INVALID_REFUND_AMOUNT("BUS_149", "Computed refund amount is invalid"),
        REFUND_PAYMENT_NOT_FOUND("BUS_150", "Refund payment not found"),
        REFUND_EXCEEDS_REMAINING("BUS_151", "Refund amount exceeds remaining refundable balance"),
        PURCHASE_RETURN_NOT_FOUND("BUS_160", "Purchase return not found"),
        PURCHASE_RETURN_INVENTORY_UNAVAILABLE("BUS_161", "Inventory item is not available for return"),
        PURCHASE_RETURN_INVENTORY_SOLD("BUS_162", "Sold inventory cannot be returned to vendor"),
        PURCHASE_RETURN_ALREADY_EXISTS("BUS_163", "This inventory item has already been returned"),
        CANNOT_RETURN_EXCHANGE_INVENTORY_DIRECTLY("BUS_164", "Trade-in vehicles cannot be returned via purchase return; handle via sale return"),
        INVALID_RETURN_AMOUNT("BUS_165", "Return amount is invalid"),
        PURCHASE_RETURN_RECEIPT_NOT_FOUND("BUS_166", "Purchase return receipt not found"),
        RECEIPT_EXCEEDS_REMAINING("BUS_167", "Receipt amount exceeds remaining receivable balance"),
        REFUND_EXCEEDS_PAID_TO_VENDOR("BUS_168", "Refund amount cannot exceed what was paid to vendor"),
        EXPENSE_PAYMENT_ACCOUNT_REQUIRED("BUS_169", "Payment account is required to record an expense");

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
