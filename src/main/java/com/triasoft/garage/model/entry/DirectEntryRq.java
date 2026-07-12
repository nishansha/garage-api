package com.triasoft.garage.model.entry;

import com.triasoft.garage.concurrency.Versioned;
import com.triasoft.garage.constants.TransactionDirectionEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DirectEntryRq implements Serializable, Versioned {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Optimistic-lock version the client last read (required on update). */
    private Long version;
    private LocalDate entryDate;
    private Long coaId;
    private TransactionDirectionEnum direction;
    private BigDecimal amount;
    private Long paymentAccountId;
    private String partyName;
    private String referenceNo;
    private String description;
    private String notes;

}
