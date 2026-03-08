package com.triasoft.garage.model.purchase;

import com.triasoft.garage.dto.PurchaseDTO;
import com.triasoft.garage.model.common.GenericRs;
import lombok.*;

import java.io.Serial;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PurchaseRs extends GenericRs {
    @Serial
    private static final long serialVersionUID = 4397989877210898284L;
    private List<PurchaseDTO> purchases;
}
