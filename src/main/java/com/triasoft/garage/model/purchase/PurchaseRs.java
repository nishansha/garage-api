package com.triasoft.garage.model.purchase;

import com.triasoft.garage.dto.PurchaseDTO;
import com.triasoft.garage.model.common.PagedRs;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;


@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class PurchaseRs extends PagedRs {
    @Serial
    private static final long serialVersionUID = 4397989877210898284L;
    private List<PurchaseDTO> purchases;
}
