package com.triasoft.garage.model.sale;

import com.triasoft.garage.constants.ExchangeHandlingEnum;
import com.triasoft.garage.validation.NullOrNotBlank;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SaleReturnRq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private LocalDate returnDate;

    @Size(max = 255, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String reason;

    @Size(max = 500, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String notes;

    private ExchangeHandlingEnum exchangeHandling;
    private BigDecimal exchangeBuybackAmount;

    @Valid
    private List<ReturnDeductionRq> soldVehicleDeductions;

    @Valid
    private List<ReturnDeductionRq> exchangeVehicleDeductions;
}
