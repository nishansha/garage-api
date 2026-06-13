package com.triasoft.garage.dto;

import com.triasoft.garage.constants.TransactionDirectionEnum;
import com.triasoft.garage.constants.TransactionTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 7261038491827364501L;

    private Long id;
    private LocalDate transactionDate;
    private TransactionTypeEnum type;
    private String referenceType;
    private Long referenceId;
    private Long paymentAccountId;
    private String paymentAccountName;
    private BigDecimal amount;
    private TransactionDirectionEnum direction;
    private String description;
    private String notes;
    private Long reversalOfId;
    private boolean isReversed;
    private boolean reconciled;
    private LocalDate reconciledAt;
    private LocalDateTime createdAt;

}
