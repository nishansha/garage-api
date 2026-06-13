package com.triasoft.garage.model.journal;

import com.triasoft.garage.dto.JournalDTO;
import com.triasoft.garage.model.common.GenericRs;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class JournalListRs extends GenericRs {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<JournalDTO> journals;

}
