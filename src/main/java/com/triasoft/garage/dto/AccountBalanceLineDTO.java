package com.triasoft.garage.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class AccountBalanceLineDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long accountId;
    private String code;
    private String label;
    private String type;
    private BigDecimal balance;

}
