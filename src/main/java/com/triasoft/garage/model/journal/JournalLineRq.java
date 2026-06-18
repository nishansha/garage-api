package com.triasoft.garage.model.journal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JournalLineRq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long accountId;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private String description;

}
