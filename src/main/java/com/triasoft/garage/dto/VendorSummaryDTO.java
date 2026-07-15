package com.triasoft.garage.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class VendorSummaryDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 8146093481227374619L;
    private Long id;
    private String name;
    private String mobile;
    private String address;
    private BigDecimal outstandingBalance;
}
