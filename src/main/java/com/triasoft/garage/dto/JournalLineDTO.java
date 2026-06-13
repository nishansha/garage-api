package com.triasoft.garage.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class JournalLineDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long accountId;
    private String accountCode;
    private String accountName;
    private String accountLabel;
    private String accountType;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private String description;

}
