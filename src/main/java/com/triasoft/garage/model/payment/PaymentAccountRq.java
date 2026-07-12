package com.triasoft.garage.model.payment;

import com.triasoft.garage.concurrency.Versioned;
import com.triasoft.garage.constants.AccountTypeEnum;
import com.triasoft.garage.validation.NullOrNotBlank;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentAccountRq implements Serializable, Versioned {

    @Serial
    private static final long serialVersionUID = 5019283746120938471L;

    private Long version;

    @NotBlank(message = "REQUIRED")
    @Size(max = 100, message = "MAX_LENGTH")
    private String name;

    @Size(max = 100, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String bankName;

    @Size(max = 20, message = "MAX_LENGTH")
    @Pattern(regexp = "^[0-9]+$", message = "INVALID_FORMAT")
    @NullOrNotBlank
    private String accountNo;

    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "INVALID_FORMAT")
    @NullOrNotBlank
    private String ifscCode;

    @NotNull(message = "REQUIRED")
    private AccountTypeEnum accountType;

    private BigDecimal openingBalance;
    private LocalDate openingDate;
    private Boolean isActive;

}
