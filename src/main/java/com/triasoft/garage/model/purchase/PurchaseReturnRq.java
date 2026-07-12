package com.triasoft.garage.model.purchase;

import com.triasoft.garage.validation.NullOrNotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PurchaseReturnRq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private LocalDate returnDate;

    @Size(max = 255, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String reason;

    @Size(max = 500, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String notes;

    private BigDecimal refundAmount;
}
