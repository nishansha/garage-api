package com.triasoft.garage.dto;

import com.triasoft.garage.constants.JournalStatusEnum;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class JournalDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private LocalDate journalDate;
    private String referenceType;
    private Long referenceId;
    private String description;
    private JournalStatusEnum status;
    private Long reversalOfId;
    private BigDecimal totalAmount;
    private int lineCount;
    private LocalDateTime createdAt;

}
