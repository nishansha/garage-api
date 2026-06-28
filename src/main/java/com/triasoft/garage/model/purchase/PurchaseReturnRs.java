package com.triasoft.garage.model.purchase;

import com.triasoft.garage.dto.PurchaseReturnDTO;
import com.triasoft.garage.model.common.GenericRs;
import lombok.*;

import java.io.Serial;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PurchaseReturnRs extends GenericRs {
    @Serial
    private static final long serialVersionUID = 3824661106050122147L;
    private Long id;
    private List<PurchaseReturnDTO> purchasesReturns;
}
