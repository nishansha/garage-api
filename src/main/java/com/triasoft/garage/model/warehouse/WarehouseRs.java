package com.triasoft.garage.model.warehouse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.triasoft.garage.dto.WarehouseDTO;
import com.triasoft.garage.model.common.GenericRs;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WarehouseRs extends GenericRs {

    @Serial
    private static final long serialVersionUID = 1928374650918273645L;

    private Long id;
    private WarehouseDTO warehouse;
    private List<WarehouseDTO> warehouses;

}
