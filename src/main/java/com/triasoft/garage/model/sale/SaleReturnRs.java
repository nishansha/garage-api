package com.triasoft.garage.model.sale;

import com.triasoft.garage.dto.SaleReturnDTO;
import com.triasoft.garage.model.common.GenericRs;
import lombok.*;

import java.io.Serial;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SaleReturnRs extends GenericRs {
    @Serial
    private static final long serialVersionUID = -8070183366645814889L;
    private Long id;
    private List<SaleReturnDTO> saleReturns;
}
