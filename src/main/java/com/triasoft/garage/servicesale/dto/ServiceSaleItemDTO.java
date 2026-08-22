package com.triasoft.garage.servicesale.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class ServiceSaleItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long serviceOfferingId;
    private String description;
    private BigDecimal qty;
    private BigDecimal rate;
    private BigDecimal amount;
}
