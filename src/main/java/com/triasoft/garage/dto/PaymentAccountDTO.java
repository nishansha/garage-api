package com.triasoft.garage.dto;

import com.triasoft.garage.constants.AccountTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class PaymentAccountDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 3920184756123049182L;

    private Long id;
    private String name;
    private String bankName;
    private String accountNo;
    private String ifscCode;
    private AccountTypeEnum accountType;
    private BigDecimal openingBalance;
    private BigDecimal currentBalance;
    private boolean isActive;

}
