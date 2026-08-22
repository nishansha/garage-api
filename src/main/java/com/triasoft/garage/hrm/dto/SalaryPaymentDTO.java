package com.triasoft.garage.hrm.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class SalaryPaymentDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private int payPeriodMonth;
    private int payPeriodYear;
    private BigDecimal grossAmount;
    private BigDecimal netAmount;
    private LocalDate paymentDate;
    private Long paymentAccountId;
    private String paymentAccountName;
    private String status;
    private String notes;
}
