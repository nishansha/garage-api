package com.triasoft.garage.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class StockDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 7536597314652294563L;
    private Long productId;
    private LocalDate purchaseDate;
    private String productCode;
    private String brandName;
    private String modelName;
    private String variantName;
    private BigDecimal totalAmount;
    private BigDecimal purchasedAmount;
    private BigDecimal purchaseExpense;
    private String status;
    private String color;
    private Long odometer;

}
