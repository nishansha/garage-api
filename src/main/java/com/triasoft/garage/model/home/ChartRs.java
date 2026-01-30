package com.triasoft.garage.model.home;

import com.triasoft.garage.dto.DataInfo;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Builder
@Data
public class ChartRs implements Serializable {
    @Serial
    private static final long serialVersionUID = -4128956425652316541L;
    private List<DataInfo> topProducts;
}
