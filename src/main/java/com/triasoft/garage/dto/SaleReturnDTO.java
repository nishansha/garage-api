package com.triasoft.garage.dto;

import com.triasoft.garage.constants.ExchangeHandlingEnum;
import com.triasoft.garage.constants.ReturnStatusEnum;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class SaleReturnDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = -2373108476663992252L;
    private Long id;
    private Long saleId;
    private String invoiceNo;
    private LocalDate returnDate;
    private String reason;
    private String notes;
    private BigDecimal customerPaidAmount;
    private ExchangeHandlingEnum exchangeHandling;
    private BigDecimal exchangeBuybackAmount;
    private BigDecimal soldVehicleDeductionAmount;
    private BigDecimal exchangeVehicleDeductionAmount;
    private BigDecimal refundAmount;
    private ReturnStatusEnum status;
    private BigDecimal totalRefunded;
    private BigDecimal remainingRefund;
    private List<DeductionDTO> deductions;
    private List<RefundPaymentDTO> refunds;
}
