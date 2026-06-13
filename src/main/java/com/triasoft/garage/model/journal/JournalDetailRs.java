package com.triasoft.garage.model.journal;

import com.triasoft.garage.constants.JournalStatusEnum;
import com.triasoft.garage.dto.JournalLineDTO;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class JournalDetailRs implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private LocalDate journalDate;
    private String referenceType;
    private Long referenceId;
    private String description;
    private JournalStatusEnum status;
    private Long reversalOfId;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private LocalDateTime createdAt;
    private List<JournalLineDTO> lines;

}
