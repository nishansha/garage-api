package com.triasoft.garage.model.purchase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RcDueReceiptCreateRs implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long receiptId;
    private Long purchaseId;
    private BigDecimal amount;
    private BigDecimal totalReceived;
    private BigDecimal remainingRcDue;
}
