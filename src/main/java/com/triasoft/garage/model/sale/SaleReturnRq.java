package com.triasoft.garage.model.sale;

import com.triasoft.garage.constants.ExchangeHandlingEnum;
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
    private String reason;
    private String notes;

    private ExchangeHandlingEnum exchangeHandling;
    private BigDecimal exchangeBuybackAmount;

    private List<ReturnDeductionRq> soldVehicleDeductions;
    private List<ReturnDeductionRq> exchangeVehicleDeductions;
}
