package com.triasoft.garage.servicesale.dto;

import com.triasoft.garage.constants.PaymentMethodEnum;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ServiceSalePaymentDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private PaymentMethodEnum paymentMethod;
    private String referenceNo;
    private String notes;
    private Long paymentAccountId;
    private String paymentAccountName;
}
