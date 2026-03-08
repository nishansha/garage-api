package com.triasoft.garage.model.home;

import com.triasoft.garage.dto.SummaryInfo;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class OverviewRs implements Serializable {
    @Serial
    private static final long serialVersionUID = 4071050057835717061L;
    private List<SummaryInfo> data;
}
