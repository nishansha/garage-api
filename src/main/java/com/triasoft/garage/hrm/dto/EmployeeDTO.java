package com.triasoft.garage.hrm.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class EmployeeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long version;
    private Long companyId;
    private String employeeCode;
    private String name;
    private String designation;
    private LocalDate joinDate;
    private LocalDate terminationDate;
    private BigDecimal salaryAmount;
    private String bankName;
    private String bankAccountNo;
    private Long paymentAccountId;
    private String paymentAccountName;
    private Long userProfileId;
    private boolean active;
}
