package com.triasoft.garage.model.sale;

import com.triasoft.garage.locking.Versioned;
import com.triasoft.garage.dto.PurchaseDTO;
import com.triasoft.garage.dto.SaleAmountSplitDTO;
import com.triasoft.garage.validation.NullOrNotBlank;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class SalesRq implements Serializable, Versioned {
    @Serial
    private static final long serialVersionUID = -1184333388893688185L;


    private Long id;
    private Long version;

    @NotNull(message = "REQUIRED")
    private LocalDate date;

    @NotNull(message = "REQUIRED")
    private Long stockId;

    @NotNull(message = "REQUIRED")
    @DecimalMin(value = "0.0", inclusive = false, message = "MUST_BE_POSITIVE")
    private BigDecimal saleRate;

    @NotBlank(message = "REQUIRED")
    @Size(max = 100, message = "MAX_LENGTH")
    @Pattern(regexp = "^[A-Za-z .]+$", message = "INVALID_CHARS")
    private String customerName;

    @NotBlank(message = "REQUIRED")
    @Pattern(regexp = "^[0-9]{10}$", message = "INVALID_FORMAT")
    private String customerMobileNo;

    @Size(max = 255, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String customerAddress;

    private boolean isExchanged;
    private BigDecimal exchangeAmount;
    private PurchaseDTO exchangeVehicleDetails;

    private boolean isFinanced;

    @Size(max = 100, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String financeCompany;

    private BigDecimal financeAmount;
    private BigDecimal emiAmount;

    private Long statusId;

    @Valid
    private List<SaleAmountSplitDTO> amountSplits;

}
