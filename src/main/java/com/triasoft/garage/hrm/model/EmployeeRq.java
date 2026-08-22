package com.triasoft.garage.hrm.model;

import com.triasoft.garage.locking.Versioned;
import com.triasoft.garage.validation.NullOrNotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EmployeeRq implements Serializable, Versioned {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long version;

    @NotNull(message = "REQUIRED")
    private Long companyId;

    @NotBlank(message = "REQUIRED")
    @Size(max = 50, message = "MAX_LENGTH")
    private String employeeCode;

    @NotBlank(message = "REQUIRED")
    @Size(max = 255, message = "MAX_LENGTH")
    private String name;

    @Size(max = 100, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String designation;

    @NotNull(message = "REQUIRED")
    private LocalDate joinDate;

    private LocalDate terminationDate;

    @NotNull(message = "REQUIRED")
    @DecimalMin(value = "0.0", inclusive = false, message = "MUST_BE_POSITIVE")
    private BigDecimal salaryAmount;

    @Size(max = 100, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String bankName;

    @Size(max = 50, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String bankAccountNo;

    @NotNull(message = "REQUIRED")
    private Long paymentAccountId;

    // Optional — only set when this employee also has system login access.
    private Long userProfileId;

    private boolean active = true;
}
