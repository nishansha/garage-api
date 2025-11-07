package com.triasoft.garage.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class CategoryDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 4726843667963762888L;
    private Long id;
    private String code;
    private String description;
}
