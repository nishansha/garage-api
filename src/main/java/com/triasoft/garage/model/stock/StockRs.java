package com.triasoft.garage.model.stock;

import com.triasoft.garage.dto.StockDTO;
import com.triasoft.garage.model.common.PagedRs;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;


@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class StockRs extends PagedRs {
    @Serial
    private static final long serialVersionUID = -8314068862889914995L;
    private List<StockDTO> products;
}
