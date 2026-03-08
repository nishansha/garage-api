package com.triasoft.garage.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LookupRq implements Serializable {

    @Serial
    private static final long serialVersionUID = -7198891127617763664L;
    private Long id;
    private String type;
    private String code;
    private String description;

}
