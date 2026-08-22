package com.triasoft.garage.servicesale.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ServiceSaleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long version;
    private String invoiceNo;
    private Long companyId;
    private Long warehouseId;
    private Long customerId;
    private String customerName;
    private String walkInCustomerName;
    private LocalDate saleDate;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String paymentStatus;
    private String notes;
    private List<ServiceSaleItemDTO> items;
    private List<ServiceSalePaymentDTO> payments;
}
