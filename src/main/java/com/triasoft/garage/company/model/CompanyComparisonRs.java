package com.triasoft.garage.company.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class CompanyComparisonRs implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String month;
    private List<CompanyPerformanceInfo> companies;
}
