package com.triasoft.garage.model.payment;

import com.triasoft.garage.concurrency.Versioned;
import com.triasoft.garage.constants.AccountTypeEnum;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentAccountRq implements Serializable, Versioned {

    @Serial
    private static final long serialVersionUID = 5019283746120938471L;

    /** Optimistic-lock version the client last read (required on update). */
    private Long version;
    private String name;
    private String bankName;
    private String accountNo;
    private String ifscCode;
    private AccountTypeEnum accountType;
    private BigDecimal openingBalance;
    private LocalDate openingDate;
    private Boolean isActive;

}
