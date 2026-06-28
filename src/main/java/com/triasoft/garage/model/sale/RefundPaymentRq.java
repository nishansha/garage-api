package com.triasoft.garage.model.sale;

import com.triasoft.garage.constants.PaymentMethodEnum;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RefundPaymentRq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private BigDecimal amount;
    private LocalDate paymentDate;
    private PaymentMethodEnum paymentMethod;
    private Long paymentAccountId;
    private String referenceNo;
    private String notes;
}
