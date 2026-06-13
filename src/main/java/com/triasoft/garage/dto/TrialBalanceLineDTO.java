package com.triasoft.garage.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class TrialBalanceLineDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long accountId;
    private String code;
    private String name;
    private String label;
    private String type;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private BigDecimal netBalance;
    private String balanceSide;   // "DR" or "CR"

}
