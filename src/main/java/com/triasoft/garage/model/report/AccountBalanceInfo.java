package com.triasoft.garage.model.report;

import com.triasoft.garage.constants.AccountTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AccountBalanceInfo {
    private Long id;
    private String name;
    private AccountTypeEnum accountType;
    private BigDecimal balance;
}
