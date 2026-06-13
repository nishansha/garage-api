package com.triasoft.garage.model.report;

import com.triasoft.garage.dto.AccountBalanceLineDTO;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class BalanceSheetRs implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private LocalDate asOfDate;

    private Section assets;
    private Section liabilities;
    private EquitySection equity;

    private BigDecimal totalLiabilitiesAndEquity;
    private boolean isBalanced;

    @Data
    @Builder
    public static class Section implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private List<AccountBalanceLineDTO> accounts;
        private BigDecimal total;
    }

    @Data
    @Builder
    public static class EquitySection implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private List<AccountBalanceLineDTO> accounts;
        private BigDecimal currentYearEarnings;
        private BigDecimal total;
    }

}
