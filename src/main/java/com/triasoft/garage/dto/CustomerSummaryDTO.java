package com.triasoft.garage.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class CustomerSummaryDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 3471905826140937712L;
    private Long id;
    private String name;
    private String mobile;
    private String address;
    private BigDecimal outstandingBalance;
}
