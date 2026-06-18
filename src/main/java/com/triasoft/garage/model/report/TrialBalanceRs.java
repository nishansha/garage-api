package com.triasoft.garage.model.report;

import com.triasoft.garage.dto.TrialBalanceLineDTO;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class TrialBalanceRs implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private LocalDate asOfDate;
    private List<TrialBalanceLineDTO> lines;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private boolean isBalanced;

}
