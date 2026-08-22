package com.triasoft.garage.company.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.triasoft.garage.company.dto.CompanyDTO;
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
public class CompanyRs extends GenericRs {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private CompanyDTO company;
    private List<CompanyDTO> companies;
}
