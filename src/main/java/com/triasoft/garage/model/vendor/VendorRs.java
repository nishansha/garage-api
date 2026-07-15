package com.triasoft.garage.model.vendor;

import com.triasoft.garage.dto.VendorSummaryDTO;
import com.triasoft.garage.model.common.GenericRs;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class VendorRs extends GenericRs {
    @Serial
    private static final long serialVersionUID = -1907447129843726014L;
    private List<VendorSummaryDTO> vendors;
}
