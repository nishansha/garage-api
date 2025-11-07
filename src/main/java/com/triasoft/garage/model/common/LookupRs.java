package com.triasoft.garage.model.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.triasoft.garage.dto.LookupDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LookupRs implements Serializable {
    @Serial
    private static final long serialVersionUID = -7574272546745883740L;
    private LookupDTO value;
    private List<LookupDTO> values;
}
