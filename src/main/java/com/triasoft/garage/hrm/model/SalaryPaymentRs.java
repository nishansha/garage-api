package com.triasoft.garage.hrm.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.triasoft.garage.hrm.dto.SalaryPaymentDTO;
import com.triasoft.garage.model.common.GenericRs;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SalaryPaymentRs extends GenericRs {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private List<SalaryPaymentDTO> salaryPayments;
}
