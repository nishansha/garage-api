package com.triasoft.garage.model.purchase;

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
public class PurchasePaymentRq implements Serializable {

    @Serial
    private static final long serialVersionUID = -2947163820591748563L;

    private BigDecimal amount;
    private LocalDate paymentDate;
    private PaymentMethodEnum paymentMethod;
    private String referenceNo;
    private String notes;

}
