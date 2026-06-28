package com.triasoft.garage.model.purchase;

import com.triasoft.garage.constants.ReturnStatusEnum;
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
public class ReceiptCreateRs implements Serializable {
    @Serial
    private static final long serialVersionUID = -8820077129748051677L;
    private Long receiptId;
    private Long purchaseReturnId;
    private BigDecimal amount;
    private BigDecimal totalReceived;
    private BigDecimal remainingReceivable;
    private ReturnStatusEnum status;
}
