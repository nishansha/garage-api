package com.triasoft.garage.model.journal;

import com.triasoft.garage.dto.AccountBalanceLineDTO;
import com.triasoft.garage.dto.LedgerLineDTO;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class LedgerRs implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private AccountBalanceLineDTO account;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal openingBalance;
    private String openingBalanceSide;
    private BigDecimal closingBalance;
    private String closingBalanceSide;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private List<LedgerLineDTO> lines;

}
