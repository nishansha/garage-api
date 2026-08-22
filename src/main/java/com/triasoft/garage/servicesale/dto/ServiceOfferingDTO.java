package com.triasoft.garage.servicesale.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class ServiceOfferingDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long version;
    private Long warehouseId;
    private String code;
    private String name;
    private BigDecimal defaultRate;
    private boolean active;
}
