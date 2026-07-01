package com.triasoft.garage.constants;

/**
 * System-defined Chart of Accounts roles. Each role maps to exactly one row in
 * {@code fnd_chart_of_accounts.system_role}. Lets the journal subsystem and the
 * dashboard queries pin on stable role names instead of user-editable codes.
 */
public enum SystemCoaRole {
    AR,
    FINANCE_RECEIVABLE,
    VENDOR_REFUND_RECEIVABLE,
    INVENTORY,
    AP,
    CUSTOMER_SETTLEMENT_PAYABLE,
    CUSTOMER_REFUND_PAYABLE,
    OPENING_BALANCE_EQUITY,
    SALES_REVENUE,
    RETURN_DEDUCTION_INCOME,
    GAIN_ON_EXCHANGE_ADJ,
    COGS,
    LOSS_RETURNED_EXCHANGE,
    LOSS_PURCHASE_RETURN
}
