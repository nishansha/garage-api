package com.triasoft.garage.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class LedgerLineDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long journalId;
    private LocalDate journalDate;
    private String referenceType;
    private Long referenceId;
    private String description;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal runningBalance;
    private String runningBalanceSide;

}
