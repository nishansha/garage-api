package com.triasoft.garage.dto;

import com.triasoft.garage.company.constants.BusinessLine;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

@Data
@Builder
public class WarehouseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 8213746192837465012L;

    private Long id;
    private Long version;
    private Long companyId;
    private Set<BusinessLine> businessLines;
    private String code;
    private String name;
    private String address;
    private String location;

}
