package com.triasoft.garage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LookupDTO  implements Serializable {
    @Serial
    private static final long serialVersionUID = -4008817912066778098L;

    private Long id;
    private String code;
    private String description;
}
