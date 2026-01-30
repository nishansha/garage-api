package com.triasoft.garage.model.common;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Data
public class FilterRq implements Serializable {
    @Serial
    private static final long serialVersionUID = -4624231141335110518L;
    private String searchText;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String vehicleNo;
    private String staffId;
    private String brandId;
    private String modelId;
    private String variantId;
    private String typeId;

}
