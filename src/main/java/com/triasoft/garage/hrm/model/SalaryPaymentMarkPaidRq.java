package com.triasoft.garage.hrm.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Data
public class SalaryPaymentMarkPaidRq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "REQUIRED")
    private LocalDate paymentDate;

    @NotNull(message = "REQUIRED")
    private Long paymentAccountId;
}
