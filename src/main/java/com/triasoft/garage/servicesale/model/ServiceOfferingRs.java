package com.triasoft.garage.servicesale.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.triasoft.garage.model.common.GenericRs;
import com.triasoft.garage.servicesale.dto.ServiceOfferingDTO;
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
public class ServiceOfferingRs extends GenericRs {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private ServiceOfferingDTO serviceOffering;
    private List<ServiceOfferingDTO> services;
}
