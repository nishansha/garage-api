package com.triasoft.garage.model.payment;

import com.triasoft.garage.constants.AccountTypeEnum;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class PaymentAccountRq implements Serializable {

    @Serial
    private static final long serialVersionUID = 5019283746120938471L;

    private String name;
    private String bankName;
    private String accountNo;
    private String ifscCode;
    private AccountTypeEnum accountType;
    private BigDecimal openingBalance;
    private Boolean isActive;

}
