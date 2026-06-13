package com.triasoft.garage.dto;

import com.triasoft.garage.constants.TransactionDirectionEnum;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DirectEntryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private LocalDate entryDate;
    private Long typeId;
    private String typeCode;
    private String typeName;
    private TransactionDirectionEnum direction;
    private BigDecimal amount;
    private Long paymentAccountId;
    private String paymentAccountName;
    private String partyName;
    private String referenceNo;
    private String description;
    private String notes;
    private LocalDateTime createdAt;

}
