package com.triasoft.garage.model.sale;

import com.triasoft.garage.dto.PurchaseDTO;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class SalesRq implements Serializable {
    @Serial
    private static final long serialVersionUID = -1184333388893688185L;
    private Long id;
    private LocalDate date;
    private Long stockId;
    private BigDecimal saleRate;

    private String paymentStatus;
    private String customerName;
    private String customerMobileNo;
    private String customerAddress;
    private boolean isExchanged;
    private BigDecimal exchangeAmount;
    private PurchaseDTO exchangeVehicleDetails;

    private boolean isFinanced;
    private String financeCompany;
    private BigDecimal financeAmount;
    private BigDecimal emiAmount;

}
