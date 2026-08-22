package com.triasoft.garage.servicesale.model;

import com.triasoft.garage.locking.Versioned;
import com.triasoft.garage.validation.NullOrNotBlank;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
public class ServiceSaleRq implements Serializable, Versioned {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long version;

    @NotNull(message = "REQUIRED")
    private Long warehouseId;

    // Either an existing customer or a walk-in name — validated (not annotation-level,
    // since it's an either/or) in ServiceSaleService.
    private Long customerId;

    @Size(max = 255, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String walkInCustomerName;

    @NotNull(message = "REQUIRED")
    private LocalDate saleDate;

    @Size(max = 500, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String notes;

    @Valid
    @NotEmpty(message = "REQUIRED")
    private List<ServiceSaleItemRq> items;
}
