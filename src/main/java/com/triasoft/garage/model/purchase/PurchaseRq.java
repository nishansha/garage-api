package com.triasoft.garage.model.purchase;

import com.triasoft.garage.dto.ExpenseDTO;
import com.triasoft.garage.model.common.GenericRq;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PurchaseRq extends GenericRq {
    
    @Serial
    private static final long serialVersionUID = 5738931391416021178L;

    private Long id;
    private LocalDate date;
    private String vehicleNo;
    private String code;
    private Long warehouseId; //new
    private Long segmentId; //new
    private Long brandId;
    private Long modelId;
    private Long variantId;
    private String makeYear;
    private String odometer;
    private String color; //new
    private BigDecimal purchaseRate;
    private Long pickupStaffId;
    private String pickupLocation;
    private String notes;

    private String ownerName;
    private String ownerMobileNo;
    private String ownerShipSerialNo;
    private Long sourceSaleId;

    private List<ExpenseDTO> expenses;
}
