package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SalaryPayableInfo {
    private Long salaryPaymentId;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private int payPeriodMonth;
    private int payPeriodYear;
    private BigDecimal amount;
    private BigDecimal pendingAmount;
}
