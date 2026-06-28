package com.triasoft.garage.dto;

import com.triasoft.garage.constants.DeductionContextEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeductionDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private DeductionContextEnum vehicleContext;
    private Long expenseId;
    private String description;
    private BigDecimal amount;
}
