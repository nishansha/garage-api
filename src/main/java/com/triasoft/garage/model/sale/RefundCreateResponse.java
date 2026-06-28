package com.triasoft.garage.model.sale;

import com.triasoft.garage.constants.PaymentMethodEnum;
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
public class RefundCreateResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 3534767929289606396L;
    private Long refundPaymentId;
    private Long saleReturnId;
    private BigDecimal amount;
    private PaymentMethodEnum paymentMethod;
    private BigDecimal totalRefunded;
    private BigDecimal remainingRefund;
    private ReturnStatusEnum saleReturnStatus;
}
