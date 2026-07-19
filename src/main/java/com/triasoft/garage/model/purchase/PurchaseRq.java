package com.triasoft.garage.model.purchase;

import com.triasoft.garage.concurrency.Versioned;
import com.triasoft.garage.dto.ExpenseDTO;
import com.triasoft.garage.model.common.GenericRq;
import com.triasoft.garage.validation.NullOrNotBlank;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class PurchaseRq extends GenericRq implements Versioned {

    @Serial
    private static final long serialVersionUID = 5738931391416021178L;

    private Long id;
    private Long version;

    @NotNull(message = "REQUIRED")
    private LocalDate date;

    private LocalDate deliveredDate;

    @NotBlank(message = "REQUIRED")
    @Size(max = 20, message = "MAX_LENGTH")
    @Pattern(regexp = "^[A-Za-z0-9 -]+$", message = "INVALID_CHARS")
    private String vehicleNo;

    @Size(max = 50, message = "MAX_LENGTH")
    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "INVALID_CHARS")
    private String code;

    private Long warehouseId;
    private Long segmentId;

    @NotNull(message = "REQUIRED")
    private Long brandId;

    @NotNull(message = "REQUIRED")
    private Long modelId;

    @NotNull(message = "REQUIRED")
    private Long variantId;

    private Long fuelTypeId;

    private Long transmissionTypeId;

    @Pattern(regexp = "^[0-9]{4}$", message = "INVALID_FORMAT")
    private String makeYear;

    @Size(max = 7, message = "MAX_LENGTH")
    @Pattern(regexp = "^[0-9]+$", message = "INVALID_FORMAT")
    private String odometer;

    private Long colorId;

    @NotNull(message = "REQUIRED")
    @DecimalMin(value = "0.0", inclusive = false, message = "MUST_BE_POSITIVE")
    private BigDecimal purchaseRate;

    private Long pickupStaffId;

    @Size(max = 100, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String pickupLocation;

    @Size(max = 500, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String notes;

    @NotBlank(message = "REQUIRED")
    @Size(max = 100, message = "MAX_LENGTH")
    @Pattern(regexp = "^[A-Za-z .]+$", message = "INVALID_CHARS")
    private String ownerName;

    @NotBlank(message = "REQUIRED")
    @Pattern(regexp = "^[0-9]{10}$", message = "INVALID_FORMAT")
    private String ownerMobileNo;

    @Size(max = 50, message = "MAX_LENGTH")
    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "INVALID_CHARS")
    private String ownerShipSerialNo;

    private Long sourceSaleId;

    @Valid
    private List<ExpenseDTO> expenses;
}
