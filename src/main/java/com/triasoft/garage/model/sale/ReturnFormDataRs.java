package com.triasoft.garage.model.sale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReturnFormDataRs {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExpenseItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long expenseId;
        private String description;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VehicleInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long inventoryId;
        private String uin;
        private String vehicleNo;
        private BigDecimal landedCost;
        private List<ExpenseItem> expenses;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExchangeInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long purchaseId;
        private Long inventoryId;
        private String uin;
        private String vehicleNo;
        private BigDecimal originalExchangeAmount;
        private BigDecimal currentLandedCost;
        private List<ExpenseItem> expenses;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Body implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long saleId;
        private String invoiceNo;
        private LocalDate saleDate;
        private BigDecimal saleRate;
        private BigDecimal customerPaidAmount;
        private boolean isFinanced;
        private boolean isExchanged;
        private VehicleInfo soldVehicle;
        private ExchangeInfo exchangeVehicle;
    }
}
