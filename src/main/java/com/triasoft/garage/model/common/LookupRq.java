package com.triasoft.garage.model.common;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "REQUIRED")
    private String type;

    @NotBlank(message = "REQUIRED")
    private String code;

    @NotBlank(message = "REQUIRED")
    private String description;

}
