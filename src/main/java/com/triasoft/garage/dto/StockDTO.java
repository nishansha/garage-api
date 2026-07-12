package com.triasoft.garage.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class StockDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 7536597314652294563L;
    private Long productId;
    private String vendorName;
    private String vendorMobileNo;
    private LocalDate purchaseDate;
    private String productCode;
    private String brandName;
    private String modelName;
    private String variantName;
    private Long fuelTypeId;
    private String fuelType;
    private BigDecimal totalAmount;
    private BigDecimal purchasedAmount;
    private BigDecimal purchaseExpense;
    private String status;
    private String color;
    private Long odometer;
    private BigDecimal landedCost;
    private BigDecimal soldAmount;
    private BigDecimal saleRate;
    private BigDecimal profit;
    private LocalDate soldDate;
    private String customerName;
    private String customerMobileNo;
    private List<ExpenseDTO> expenses;
}
