package com.triasoft.garage.model.sale;

import com.triasoft.garage.constants.PayerTypeEnum;
import com.triasoft.garage.constants.PaymentMethodEnum;
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
public class SalePaymentRq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private BigDecimal amount;
    private LocalDate paymentDate;
    private PaymentMethodEnum paymentMethod;
    private PayerTypeEnum payerType;
    private Long paymentAccountId;
    private String referenceNo;
    private String notes;

}
