package com.triasoft.garage.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class SaleDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 3200483156091203742L;
    private Long id;
    private LocalDate date;
    private String customerName;
    private String customerMobileNo;
    private String vehicleNo;
    private String brandName;
    private String modelName;
    private String variantName;
    private BigDecimal saleRate;
    private BigDecimal profit;
    private boolean isExchange;
    private boolean isFinanced;



}
