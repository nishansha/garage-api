package com.triasoft.garage.dto;

import com.triasoft.garage.constants.StatusEnum;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PurchaseDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = -5333887634226778331L;
    private Long id;
    private LocalDate date;
    private LocalDate deliveredDate;
    private String code;
    private String vehicleNo;
    private Long warehouseId; //new
    private Long segmentId; //new
    private Long brandId;
    private Long modelId;
    private Long variantId;
    private String brandName;
    private String modelName;
    private String variantName;
    private String makeYear;
    private String odometer;
    private Long colorId;
    private String colorName;
    private String segmentName;
    private BigDecimal purchaseRate;
    private BigDecimal totalExpenses;
    private BigDecimal totalCost;
    private BigDecimal paidAmount;
    private BigDecimal pendingAmount;
    private StatusEnum paymentStatus;
    private Long pickupStaffId;
    private String pickupLocation;
    private String notes;
    private boolean isSold;
    private boolean isEditable;
    private Long sourceSaleId;

    private String ownerName;
    private String ownerMobileNo;
    private String ownerShipSerialNo;
    private List<ExpenseDTO> expenses;
    private List<PurchasePaymentDTO> payments;

}
