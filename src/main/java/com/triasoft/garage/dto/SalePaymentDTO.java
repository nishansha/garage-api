package com.triasoft.garage.dto;

import com.triasoft.garage.constants.PayerTypeEnum;
import com.triasoft.garage.constants.PaymentMethodEnum;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class SalePaymentDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private PaymentMethodEnum paymentMethod;
    private PayerTypeEnum payerType;
    private Long paymentAccountId;
    private String paymentAccountName;
    private String referenceNo;
    private String notes;

}
