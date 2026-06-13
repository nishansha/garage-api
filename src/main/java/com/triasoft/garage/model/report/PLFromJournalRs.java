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
public class PLFromJournalRs implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private LocalDate fromDate;
    private LocalDate toDate;

    private Section revenue;
    private Section expenses;

    private BigDecimal netProfit;

    @Data
    @Builder
    public static class Section implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private List<AccountBalanceLineDTO> accounts;
        private BigDecimal total;
    }

}
