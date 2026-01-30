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
public class PurchaseDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = -5333887634226778331L;
    private Long id;
    private LocalDate date;
    private String vehicleNo;
    private String brandName;
    private String modelName;
    private String variantName;
    private String makeYear;
    private String drivenKilometer;
    private String ownerShipSlNo;
    private BigDecimal purchaseRate;
    private Long pickupStaffId;
    private String pickupStaff;
    private String pickupLocation;
    private boolean isSold;

    private String userMobileNo;
    private List<ExpenseDTO> expenses;

}
