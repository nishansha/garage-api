package com.triasoft.garage.model.customer;

import com.triasoft.garage.dto.CustomerSummaryDTO;
import com.triasoft.garage.model.common.GenericRs;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class CustomerRs extends GenericRs {
    @Serial
    private static final long serialVersionUID = -5822013947620185634L;
    private List<CustomerSummaryDTO> customers;
}
