package com.triasoft.garage.model.entry;

import com.triasoft.garage.dto.DirectEntryDTO;
import com.triasoft.garage.model.common.GenericRs;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class DirectEntryRs extends GenericRs {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private List<DirectEntryDTO> entries;

}
