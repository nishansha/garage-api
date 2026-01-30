package com.triasoft.garage.model.sale;

import com.triasoft.garage.dto.SaleDTO;
import com.triasoft.garage.model.common.PagedRs;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;


@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class SalesRs extends PagedRs {
    @Serial
    private static final long serialVersionUID = -8728351026302264141L;
    private List<SaleDTO> sales;
}
